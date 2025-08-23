// Simple Worker to test Kotlin/Wasm loading
// Note: This is a proof-of-concept - Kotlin/Wasm uses newer WebAssembly features

export default {
  async fetch(request, env, ctx) {
    try {
      const url = new URL(request.url);
      
      // Simple routing
      if (url.pathname === '/health') {
        return new Response('HuskerBot Worker POC is healthy! 🌽', {
          headers: { 'Content-Type': 'text/plain' }
        });
      }
      
      if (url.pathname === '/test-wasm') {
        return await testWasmLoading(request);
      }
      
      if (request.method === 'POST') {
        return await handleDiscordWebhook(request);
      }
      
      return new Response('HuskerBot Cloudflare Worker POC\n\nEndpoints:\n/health - Health check\n/test-wasm - Test WebAssembly loading', {
        headers: { 'Content-Type': 'text/plain' }
      });
      
    } catch (error) {
      console.error('Worker error:', error);
      return new Response(`Error: ${error.message}`, { status: 500 });
    }
  }
};

async function testWasmLoading(request) {
  try {
    // Try to load the Kotlin/Wasm module
    const wasmResponse = await fetch(new URL('./huskerbot-worker-poc-wasm-js.wasm', request.url));
    const wasmBytes = await wasmResponse.arrayBuffer();
    
    // Check if we can compile the module (may fail due to unsupported features)
    try {
      const module = await WebAssembly.compile(wasmBytes);
      return new Response('✅ Kotlin/Wasm module compiled successfully!\nSize: ' + wasmBytes.byteLength + ' bytes', {
        headers: { 'Content-Type': 'text/plain' }
      });
    } catch (wasmError) {
      return new Response(`❌ WebAssembly compilation failed: ${wasmError.message}\n\nThis likely means Cloudflare Workers doesn't support the WebAssembly GC proposal used by Kotlin/Wasm yet.\n\nAlternatives:\n- Use Kotlin/JS instead\n- Wait for Workers to support Wasm GC\n- Use a different language for Workers`, {
        headers: { 'Content-Type': 'text/plain' }
      });
    }
  } catch (error) {
    return new Response(`Error testing Wasm: ${error.message}`, { status: 500 });
  }
}

async function handleDiscordWebhook(request) {
  try {
    const body = await request.json();
    
    // Handle Discord ping
    if (body.type === 1) {
      return new Response(JSON.stringify({ type: 1 }), {
        headers: { 'Content-Type': 'application/json' }
      });
    }
    
    // Handle slash command
    if (body.type === 2) {
      const commandName = body.data?.name;
      let responseContent;
      
      switch (commandName) {
        case 'ping':
          responseContent = 'Pong! HuskerBot is running on Cloudflare Workers! 🚀';
          break;
        case 'gameday-weather':
          responseContent = '🏈 **Game Day Weather POC**\\n\\n🌡️ 72°F\\n☁️ Partly Cloudy\\n💨 10 mph NW\\n\\n🔥 **Hot Take**: Perfect football weather! Even if we\'re losing, at least you won\'t freeze your corn-fed butt off in Memorial Stadium! 🌽';
          break;
        default:
          responseContent = `Unknown command: ${commandName}`;
      }
      
      return new Response(JSON.stringify({
        type: 4,
        data: { content: responseContent }
      }), {
        headers: { 'Content-Type': 'application/json' }
      });
    }
    
    return new Response('Unknown interaction type', { status: 400 });
    
  } catch (error) {
    console.error('Discord webhook error:', error);
    return new Response('Error processing Discord interaction', { status: 500 });
  }
}