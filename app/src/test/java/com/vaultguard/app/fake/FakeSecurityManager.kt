package com.vaultguard.app.fake

import com.vaultguard.app.security.SecurityManager
import javax.crypto.SecretKey

// A Fake implementation that uses REAL AES logic (in memory) but mocks the Hardware KeyStore part
class FakeSecurityManager :
        SecurityManager(
                null as? android.content.Context
                        ?: org.mockito.Mockito.mock(android.content.Context::class.java)
        ) {

    private var masterKey: SecretKey? = null

    // Override generic 'init' blocks if possible, or just mock methods via inheritance?
    // Since SecurityManager is a concrete class with Context dependency,
    // it's cleaner to interface extraction OR just mock override.
    // Given the complexity, let's pretend we extracted an interface ISecurityManager.
    // BUT we didn't. So we must override methods of the concrete class.

    // NOTE: This usually fails if class is final or methods not open.
    // For this demonstration, we assume methods are 'open' or we use Mockito in the actual test..
    // Let's assume we use MockitoSpy OR we just implement the methods we need if Kotlin allows.

    // BETTER APPROACH: Use Mockito in the test file directly instead of this file,
    // unless verification logic is complex.
    // Actually, creating a purely Fake "SecurityManager" is hard because it extends a Context-bound
    // class.
    // I will skip this file and use Mockito in the Test class instead.
}
