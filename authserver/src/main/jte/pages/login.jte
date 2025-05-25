@import me.amlu.authserver.view.helper.CsrfHiddenInput

@param Boolean error = false
@param String errorMessage = null
@param String logoutMessage = null
@param String formLoginSuccess = null
@param String oauth2LoginSuccess = null
@param CsrfHiddenInput csrfHiddenInput

@template.layout.default(
content = @`
    <div class="min-h-screen flex items-center justify-center">
        <div class="max-w-md w-full space-y-8 p-8 bg-white rounded-lg shadow-md">
            <div>
                <h2 class="mt-6 text-center text-3xl font-extrabold text-gray-900">
                    Sign in to your account
                </h2>
            </div>

            @if(error)
                <div id="errorMessage" class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative"
                     role="alert">
                    <span class="block sm:inline">${errorMessage != null ? errorMessage : "An error occurred during login."}</span>
                </div>
            @endif
            @if(logoutMessage != null)
                <div id="logoutMessage"
                     class="bg-blue-100 border border-blue-400 text-blue-700 px-4 py-3 rounded relative" role="alert">
                    <span class="block sm:inline">${logoutMessage}</span>
                </div>
            @endif
            @if(formLoginSuccess != null)
                <div id="formLoginSuccessMessage"
                     class="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded relative"
                     role="alert">
                    <span class="block sm:inline">${formLoginSuccess}</span>
                </div>
            @endif
            @if(oauth2LoginSuccess != null)
                <div id="oauth2LoginSuccessMessage"
                     class="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded relative"
                     role="alert">
                    <span class="block sm:inline">${oauth2LoginSuccess}</span>
                </div>
            @endif
            <!-- Placeholder for Passkey specific messages -->
            <div id="passkeyMessage" class="hidden px-4 py-3 rounded relative" role="alert">
                <span class="block sm:inline"></span>
            </div>


            <!-- Login Form -->
            <form class="mt-8 space-y-6" action="/login" method="POST">
                ${csrfHiddenInput}
                <div class="rounded-md shadow-sm -space-y-px">
                    <div>
                        <label for="username-input" class="sr-only">Username</label>
                        <input id="username-input" name="username" type="text" required
                               class="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-t-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
                               placeholder="Username (Email)">
                    </div>
                    <div>
                        <label for="password-input" class="sr-only">Password</label>
                        <input id="password-input" name="password" type="password" required
                               class="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-b-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
                               placeholder="Password">
                    </div>
                </div>

                <div>
                    <button type="submit"
                            class="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500">
                        Sign in
                    </button>
                </div>
            </form>

            <!-- Passkey Login Button -->
            <div class="mt-4">
                <button id="loginWithPasskeyButton" type="button"
                        class="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-gray-700 bg-gray-200 hover:bg-gray-300 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500">
                    Sign in with a Passkey
                </button>
            </div>

            <!-- OAuth2 Buttons -->
            <div class="mt-6">
                <div class="relative">
                    <div class="absolute inset-0 flex items-center">
                        <div class="w-full border-t border-gray-300"></div>
                    </div>
                    <div class="relative flex justify-center text-sm">
                        <span class="px-2 bg-white text-gray-500">Or continue with</span>
                    </div>
                </div>

                <div class="mt-6 grid grid-cols-2 gap-3">
                    <a href="/oauth2/authorization/google"
                       class="w-full inline-flex items-center justify-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-500 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500">
                        <img class="h-5 w-5 mr-2" src="https://www.svgrepo.com/show/475656/google-color.svg"
                             alt="Google logo">
                        <span>Google</span>
                    </a>

                    <a href="/oauth2/authorization/github"
                       class="w-full inline-flex items-center justify-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-500 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500">
                        <svg class="h-5 w-5 mr-2" fill="currentColor" viewBox="0 0 24 24">
                            <path fill-rule="evenodd"
                                  d="M12 2C6.477 2 2 6.477 2 12c0 4.42 2.865 8.17 6.839 9.49.5.092.682-.217.682-.482 0-.237-.008-.866-.013-1.7-2.782.604-3.369-1.34-3.369-1.34-.454-1.156-1.11-1.464-1.11-1.464-.908-.62.069-.608.069-.608 1.003.07 1.531 1.03 1.531 1.03.892 1.529 2.341 1.087 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.11-4.555-4.943 0-1.091.39-1.984 1.029-2.683-.103-.253-.446-1.27.098-2.647 0 0 .84-.269 2.75 1.022A9.606 9.606 0 0112 6.82c.85.004 1.705.115 2.504.337 1.909-1.29 2.747-1.022 2.747-1.022.546 1.377.202 2.394.1 2.647.64.699 1.028 1.592 1.028 2.683 0 3.842-2.339 4.687-4.566 4.935.359.309.678.919.678 1.852 0 1.336-.012 2.415-.012 2.743 0 .267.18.578.688.48C19.137 20.167 22 16.42 22 12c0-5.523-4.477-10-10-10z"
                                  clip-rule="evenodd"></path>
                        </svg>
                        <span>GitHub</span>
                    </a>
                </div>
            </div>
            <script>
                document.addEventListener('DOMContentLoaded', (event) => {
                    const loginWithPasskeyButton = document.getElementById('loginWithPasskeyButton');
                    const passkeyMessageDiv = document.getElementById('passkeyMessage');
                    const usernameInput = document.getElementById('username-input'); // Get username input

                    // Function to display messages
                    function showPasskeyMessage(message, isError = false) {
                        passkeyMessageDiv.className = 'px-4 py-3 rounded relative'; // Reset classes
                        if (isError) {
                            passkeyMessageDiv.classList.add('bg-red-100', 'border-red-400', 'text-red-700');
                        } else {
                            passkeyMessageDiv.classList.add('bg-green-100', 'border-green-400', 'text-green-700');
                        }
                        passkeyMessageDiv.querySelector('span').textContent = message;
                        passkeyMessageDiv.classList.remove('hidden');

                        // Hide other messages
                        ['errorMessage', 'logoutMessage', 'formLoginSuccessMessage', 'oauth2LoginSuccessMessage'].forEach(id => {
                            const el = document.getElementById(id);
                            if (el) el.classList.add('hidden');
                        });
                    }

                    // Helper to get CSRF token details from the hidden input generated by JTE
                    // Helper to get CSRF token details
                    function getCsrfTokenDetails() {
                        // Directly use JTE to inject these as JavaScript string variables
                        const csrfParamName = "${csrfHiddenInput.getCsrfToken().getParameterName()}";
                        const csrfTokenValue = "${csrfHiddenInput.getCsrfToken().getToken()}";
                        const csrfHeaderName = "${csrfHiddenInput.getCsrfToken().getHeaderName()}";

                        if (csrfParamName && csrfTokenValue && csrfHeaderName) {
                            return {
                                headerName: csrfHeaderName,
                                parameterName: csrfParamName,
                                token: csrfTokenValue
                            };
                        }

                        // Fallback to DOM query if direct injection fails (less ideal but a backup)
                        // Ensure your CsrfHiddenInput helper renders an input with id="_csrf_token_input"
                        const csrfInput = document.getElementById('_csrf_token_input');
                        if (csrfInput && csrfHeaderName) { // csrfHeaderName would still come from JTE
                            return {
                                headerName: csrfHeaderName, // This still relies on the JTE variable
                                parameterName: csrfInput.name,
                                token: csrfInput.value
                            };
                        }
                        console.error('CSRF token details could not be fully determined!');
                        return null;
                    }


                    if (loginWithPasskeyButton) {
                        loginWithPasskeyButton.addEventListener('click', async () => {
                            passkeyMessageDiv.classList.add('hidden'); // Hide previous messages
                            const username = usernameInput.value; // Get username from input field

                            // Optional: Check if username is provided if your flow requires it for options
                            // if (!username) {
                            //     showPasskeyMessage('Please enter your username to log in with a passkey.', true);
                            //     return;
                            // }

                            const csrfDetails = getCsrfTokenDetails();

                            if (!csrfDetails) {
                                showPasskeyMessage('CSRF token is missing. Cannot proceed.', true);
                                return;
                            }

                            try {
                                // 1. Get WebAuthn Assertion Options from the server
                                // Spring Security's default endpoint for assertion options.
                                // It can optionally take 'username' in the request body if your RP requires it
                                // to narrow down credentials.
                                const optionsResponse = await fetch('/login/webauthn/authenticate/options', {
                                    method: 'POST',
                                    headers: {
                                        'Content-Type': 'application/json',
                                        [csrfDetails.headerName]: csrfDetails.token
                                    },
                                    // Send username if your server-side options generation needs it
                                    // Uncomment if username is needed for options
                                    body: JSON.stringify({username: username})
                                });

// Inside the try block of the loginWithPasskeyButton event listener
                                if (!optionsResponse.ok) {
                                    const errorData = await optionsResponse.json().catch(() => ({message: 'Failed to parse error response from options endpoint.'}));
                                    throw new Error(errorData.message || 'Failed to get passkey options: ' + optionsResponse.statusText);
                                }

                                const options = await optionsResponse.json();

                                // WebAuthn API expects certain fields to be ArrayBuffer.
                                // The server should send them as Base64URL encoded strings.
                                // We need to decode them.
                                if (options.challenge) {
                                    options.challenge = bufferDecode(options.challenge);
                                }
                                if (options.allowCredentials) {
                                    for (let cred of options.allowCredentials) {
                                        if (cred.id) {
                                            cred.id = bufferDecode(cred.id);
                                        }
                                    }
                                }

                                // 2. Request Assertion from Authenticator
                                const assertion = await navigator.credentials.get({publicKey: options});

                                // 3. Send Assertion to Server for Verification
                                // Prepare the assertion data to be sent to the server.
                                // WebAuthn API returns ArrayBuffers, server expects Base64URL.
                                const assertionRequest = {
                                    id: assertion.id, // This is already Base64URL from the authenticator
                                    rawId: bufferEncode(assertion.rawId),
                                    type: assertion.type,
                                    response: {
                                        clientDataJSON: bufferEncode(assertion.response.clientDataJSON),
                                        authenticatorData: bufferEncode(assertion.response.authenticatorData),
                                        signature: bufferEncode(assertion.response.signature),
                                        userHandle: assertion.response.userHandle ? bufferEncode(assertion.response.userHandle) : null,
                                    },
                                    clientExtensionResults: assertion.getClientExtensionResults()
                                };

                                const verificationResponse = await fetch('/login/webauthn/authenticate', {
                                    method: 'POST',
                                    headers: {
                                        'Content-Type': 'application/json',
                                        [csrfDetails.headerName]: csrfDetails.token
                                    },
                                    body: JSON.stringify(assertionRequest)
                                });

                                if (verificationResponse.ok) {
                                    // Successful passkey login
                                    // Spring Security will typically redirect to the default success URL
                                    // or the original requested URL.
                                    // You might not need to do anything here if the server handles the redirect.
                                    showPasskeyMessage('Passkey login successful! Redirecting...', false);
                                    // If the server doesn't redirect automatically upon successful POST,
                                    // you might need to manually redirect or update UI.
                                    // For Spring Security, a successful POST to the processing URL usually results in a redirect.
                                    // Example: window.location.href = verificationResponse.headers.get('Location') || '/dashboard';
                                    // Check your LocalSecurityConfig's .webAuthn() success handling.
                                    // If it redirects, this message might only flash briefly.
                                    // If the server returns a JSON success and expects client-side redirect:
                                    // const successData = await verificationResponse.json();
                                    // if (successData.redirectUrl) window.location.href = successData.redirectUrl;

                                    // Spring Security's WebAuthn flow typically redirects on success.
                                    // If the page reloads due to redirect, this message might not be seen long.
                                    // If no redirect, then the page stays, and you might need to update UI.
                                    // For now, assume server handles redirect.
                                    if (verificationResponse.redirected) {
                                        window.location.href = verificationResponse.url;
                                    } else {
                                        // If not redirected, check if it was a 200 OK with a success flag
                                        // This part depends on how your /login/webauthn/authenticate endpoint responds on success
                                        // For Spring Security, it usually redirects.
                                        // If it's a 200 OK without redirect, you might need to manually redirect.
                                        // For now, we assume the default behavior is a redirect.
                                        // If you see this message and no redirect, your server isn't redirecting.
                                        showPasskeyMessage('Passkey login successful! Checking for redirect...', false);
                                        // A common pattern is for the success handler to redirect.
                                        // If not, you might need to check the response for a target URL.
                                        // For now, we'll assume Spring Security handles the redirect.
                                        // If you are still on the login page, it means the POST was successful but didn't redirect.
                                        // This is unusual for Spring Security's default processing URLs.
                                        // A common success scenario is a redirect to the default success URL.
                                        // Let's assume the server handles the redirect. If not, this message will show.
                                        // If the server sends back a JSON response with a redirect URL:
                                        // const data = await verificationResponse.json();
                                        // if (data.success && data.redirectUrl) {
                                        //    window.location.href = data.redirectUrl;
                                        // }
                                        // For Spring Security, the default success handler usually redirects.
                                        // If you are still on this page, it means the server did not redirect.
                                        // This might happen if the success handler is configured to return 200 OK.
                                        // In that case, you'd typically get a JSON response indicating success.
                                        // For now, we'll just show a success message and let the user navigate.
                                        // Or, force a redirect to a known success page if the server doesn't.
                                        window.location.href = '/dashboard'; // Or your configured success URL
                                    }

// Inside the catch block for verificationResponse
                                } else {
                                    const errorData = await verificationResponse.json().catch(() => ({message: 'Failed to parse error response from authenticate endpoint.'}));
                                    throw new Error(errorData.message || 'Passkey verification failed: ' + verificationResponse.statusText);
                                }

                            } catch (err) {
                                console.error('Passkey login error:', err);
                                showPasskeyMessage(err.message || 'Passkey login failed. Please try again or use another method.', true);
                            }
                        });
                    }

                    /**
                     * Decodes a Base64URL encoded string into an ArrayBuffer.
                     * @param {string} value The Base64URL encoded string.
                     * @returns {ArrayBuffer} The decoded ArrayBuffer.
                     * @throws {Error} if the input string is not valid Base64URL or if atob fails.
                     */
                    function bufferDecode(value) {
                        if (typeof value !== 'string') {
                            console.error("bufferDecode received non-string value:", value, "Type:", typeof value);
                            throw new TypeError('Input to bufferDecode must be a string.');
                        }
                        let s = value.replace(/-/g, '+').replace(/_/g, '/');
                        let padding = '';
                        switch (s.length % 4) {
                            case 0:
                                break;
                            case 2:
                                padding = '==';
                                break;
                            case 3:
                                padding = '=';
                                break;
                            default:
                                console.error("Illegal base64url string for switch. Original value:", value, "Processed s:", s);
                                throw new Error('Illegal base64url string format: ' + value);
                        }
                        const stringToDecode = s + padding;
                        let decodedString;
                        try {
                            // console.debug("Attempting to atob:", stringToDecode); // For debugging
                            decodedString = atob(stringToDecode);
                            if (typeof decodedString !== 'string') {
                                // This should not happen if atob succeeds without error
                                console.error("atob result is not a string:", decodedString, "Type:", typeof decodedString);
                                throw new Error('Internal error: atob did not return a string.');
                            }
                        } catch (e) {
                            console.error("Error during atob. Input stringToDecode:", stringToDecode, "Original value:", value, "Error name:", e.name, "Error message:", e.message);
                            throw e; // Rethrow the original atob error (likely DOMException for invalid chars)
                        }

                        try {
                            const uint8Array = Uint8Array.from(decodedString, (char, index) => {
                                if (typeof char !== 'string') {
                                    console.error('CRITICAL: Character at index', index, 'is not a string!', char, typeof char);
                                    throw new TypeError('Character being processed by Uint8Array.from is not a string: ' + String(char));
                                }
                                // console.log('Char:', char, 'Code Unit:', char.charCodeAt(0)); // Verbose logging
                                return char.charCodeAt(0); // This is the call in question
                            });
                            return uint8Array.buffer;
                        } catch (e_uint8) {
                            console.error("Error during Uint8Array.from processing. Input decodedString length:", decodedString.length, "Error name:", e_uint8.name, "Error message:", e_uint8.message);
                            throw e_uint8;
                        }
                    }

                    function bufferEncode(value) {
                        return btoa(String.fromCharCode.apply(null, new Uint8Array(value)))
                            .replace(/\+/g, '-')
                            .replace(new RegExp('\\/', 'g'), '_')
                            .replace(/=/g, '');
                    }
                });
            </script>
        </div>
    </div>
`)
