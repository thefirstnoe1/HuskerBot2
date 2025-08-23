// Worker JavaScript wrapper that loads the Kotlin/Wasm module
import wasmModule from './kotlin-wasm-module.wasm';

export default {
  async fetch(request, env, ctx) {
    try {
      // Initialize the Wasm module
      const wasmInstance = await WebAssembly.instantiate(wasmModule, {
        // Import objects that Kotlin/Wasm might need
        env: {
          // Add any required imports here
        }
      });
      
      // Call the Kotlin function
      return wasmInstance.exports.handleRequest(request, env, ctx);
    } catch (error) {
      console.error('Worker error:', error);
      return new Response('Internal Server Error', { status: 500 });
    }
  }
};