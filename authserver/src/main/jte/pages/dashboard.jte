@import me.amlu.authserver.view.helper.CsrfHiddenInput
@import org.springframework.security.core.GrantedAuthority
@import java.util.Collection
@import me.amlu.authserver.passkey.dto.GetPasskeyDetailResponse
@import java.util.List

@param String username = ""
@param String email = null
@param Collection<? extends GrantedAuthority> authorities = null
@param CsrfHiddenInput csrfHiddenInput
@param List<GetPasskeyDetailResponse> passkeys = null

@template.layout.default(
content = @`
    <div class="min-h-screen bg-gray-100">
        <nav class="bg-white shadow-sm">
            <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div class="flex justify-between h-16">
                    <div class="flex items-center">
                        <h1 class="text-xl font-semibold">Dashboard</h1>
                    </div>
                    <div class="flex items-center space-x-4">
                        <span class="text-gray-700">Welcome, ${username}</span>
                        @if(email != null)
                            <span class="text-gray-500 text-sm">${email}</span>
                        @endif
                        <form action="/logout" method="post">
                            ${csrfHiddenInput}
                            <button type="submit"
                                    class="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700">
                                Logout
                            </button>
                        </form>
                    </div>
                </div>
            </div>
        </nav>

        <main class="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
            <div class="px-4 py-6 sm:px-0">
                <div class="bg-white shadow rounded-lg p-6">
                    <h2 class="text-2xl font-semibold text-gray-800 mb-4">User Information</h2>
                    <div class="space-y-4">
                        <div>
                            <p class="text-sm font-medium text-gray-500">Username</p>
                            <p class="mt-1 text-lg text-gray-900">${username}</p>
                        </div>
                        @if(authorities != null && !authorities.isEmpty())
                            <div>
                                <p class="text-sm font-medium text-gray-500">Roles</p>
                                <div class="mt-1 flex flex-wrap gap-2">
                                    @for(var authority : authorities)
                                        <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-sm font-medium bg-blue-100 text-blue-800">
                                        ${authority.getAuthority()}
                                    </span>
                                    @endfor
                                </div>
                            </div>
                        @endif
                        @if(email != null)
                            <div>
                                <p class="text-sm font-medium text-gray-500">Email</p>
                                <p class="mt-1 text-lg text-gray-900">${email}</p>
                            </div>
                        @endif
                    </div>
                </div>

                <!-- Passkey Management Section -->
                <div class="mt-8 bg-white shadow rounded-lg p-6">
                    <h2 class="text-2xl font-semibold text-gray-800 mb-4">Manage Your Passkeys</h2>
                    <div id="dashboardPasskeyMessage" class="hidden px-4 py-3 rounded relative mb-4" role="alert">
                        <span class="block sm:inline"></span>
                    </div>
                    <div class="mb-4">
                        <button id="addPasskeyButton"
                                class="px-4 py-2 bg-green-500 text-white rounded-md hover:bg-green-600">
                            Add a new Passkey
                        </button>
                    </div>
                    <div>
                        <h3 class="text-lg font-medium text-gray-700 mb-2">Your Registered Passkeys:</h3>
                        <ul id="passkeyList" class="space-y-2">
                            <!-- Passkeys will be listed here by JavaScript -->
                            <li id="noPasskeysMessage" class="text-gray-500">You have no passkeys registered.</li>
                        </ul>
                    </div>
                </div>
            </div>
        </main>
    </div>

    <script>
        // Helper function to decode Base64URL to ArrayBuffer
        function bufferDecode(value) {
            if (typeof value !== 'string') {
                if (value && typeof value === 'object' && typeof value.bytes === 'string') {
                    console.warn("bufferDecode received an object, using its 'bytes' property:", value);
                    value = value.bytes; // Use the 'bytes' property
                } else {
                    console.error("bufferDecode received non-string value:", value, "Type:", typeof value);
                    throw new TypeError('Input to bufferDecode must be a string or an object with a "bytes" property.');
                }
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
                decodedString = atob(stringToDecode);
            } catch (e) {
                console.error("Error during atob. Input stringToDecode:", stringToDecode, "Original value:", value, "Error:", e);
                throw e;
            }
            try {
                const uint8Array = Uint8Array.from(decodedString, c => c.charCodeAt(0));
                return uint8Array.buffer;
            } catch (e) {
                console.error("Error during Uint8Array.from processing. Input decodedString length:", decodedString.length, "Error:", e);
                throw e;
            }
        }

        // Helper function to encode ArrayBuffer to Base64URL
        function bufferEncode(value) {
            return btoa(String.fromCharCode.apply(null, new Uint8Array(value)))
                .replace(/\+/g, '-')
                .replace(new RegExp('/', 'g'), '_')
                .replace(/=/g, '');
        }

        document.addEventListener('DOMContentLoaded', () => {
            const addPasskeyButton = document.getElementById('addPasskeyButton');
            const passkeyListUl = document.getElementById('passkeyList');
            const noPasskeysMessageLi = document.getElementById('noPasskeysMessage');
            const dashboardPasskeyMessageDiv = document.getElementById('dashboardPasskeyMessage');

            function showDashboardMessage(message, isError = false) {
                dashboardPasskeyMessageDiv.className = 'px-4 py-3 rounded relative mb-4'; // Reset classes
                if (isError) {
                    dashboardPasskeyMessageDiv.classList.add('bg-red-100', 'border-red-400', 'text-red-700');
                } else {
                    dashboardPasskeyMessageDiv.classList.add('bg-green-100', 'border-green-400', 'text-green-700');
                }
                dashboardPasskeyMessageDiv.querySelector('span').textContent = message;
                dashboardPasskeyMessageDiv.classList.remove('hidden');
            }

            function getCsrfTokenDetails() {
                const csrfParamName = "${csrfHiddenInput.getCsrfToken().getParameterName()}";
                const csrfTokenValue = "${csrfHiddenInput.getCsrfToken().getToken()}";
                const csrfHeaderName = "${csrfHiddenInput.getCsrfToken().getHeaderName()}";

                if (csrfParamName && csrfTokenValue && csrfHeaderName) {
                    return {headerName: csrfHeaderName, token: csrfTokenValue};
                }
                console.error('CSRF token details could not be determined for dashboard!');
                showDashboardMessage('Critical error: CSRF token missing. Please refresh.', true);
                return null;
            }

            const csrfDetails = getCsrfTokenDetails();

            async function loadPasskeys() {
                if (!csrfDetails) return;

                try {
                    const response = await fetch('/api/profile/passkeys', {
                        method: 'GET',
                        headers: {
                            'Accept': 'application/json',
                            [csrfDetails.headerName]: csrfDetails.token
                        }
                    });

                    if (!response.ok) {
                        const errorText = await response.text();
                        throw new Error('Failed to load passkeys: ' + response.status + ' ' + response.statusText + ' - ' + errorText);
                    }
                    const loadedPasskeyData = await response.json();

                    passkeyListUl.innerHTML = '';
                    if (loadedPasskeyData.length === 0) {
                        passkeyListUl.appendChild(noPasskeysMessageLi);
                        noPasskeysMessageLi.style.display = 'list-item';
                    } else {
                        noPasskeysMessageLi.style.display = 'none';
                        loadedPasskeyData.forEach(key => {
                            const li = document.createElement('li');
                            li.className = 'p-2 border rounded-md flex justify-between items-center';
                            const friendlyNameDisplay = key.friendlyName ? key.friendlyName : 'Unnamed Passkey';
                            const createdAtDate = key.createdAt ? new Date(key.createdAt).toLocaleDateString() : 'Unknown Date';

                            li.innerHTML =
                                '<span>' + friendlyNameDisplay + ' (Added: ' + createdAtDate + ')</span>' +
                                '<button data-credential-id="' + key.credentialId + '" class="delete-passkey-button px-3 py-1 bg-red-500 text-white rounded-md hover:bg-red-600 text-sm">Delete</button>';
                            passkeyListUl.appendChild(li);
                        });
                        document.querySelectorAll('.delete-passkey-button').forEach(button => {
                            button.addEventListener('click', handleDeletePasskey);
                        });
                    }
                } catch (err) {
                    console.error('Error loading passkeys:', err);
                    showDashboardMessage(err.message || 'Could not load passkeys.', true);
                }
            }

            async function handleDeletePasskey(event) {
                if (!csrfDetails) return;

                const credentialId = event.target.dataset.credentialId;

                if (!confirm('Are you sure you want to delete passkey "' + (credentialId ? credentialId.substring(0, 10) : 'ID_UNKNOWN') + '..."?')) {
                    return;
                }

                try {
                    const response = await fetch('/api/profile/passkeys/' + encodeURIComponent(credentialId), { // Path variable
                        method: 'DELETE',
                        headers: {
                            [csrfDetails.headerName]: csrfDetails.token
                        }
                    });

                    if (!response.ok) {
                        const errorData = await response.json().catch(() => ({message: 'Failed to parse error response from delete endpoint.'}));

                        throw new Error((errorData.message || 'Failed to delete passkey: ' + response.status + ' ' + response.statusText));
                    }
                    showDashboardMessage('Passkey deleted successfully.', false);
                    loadPasskeys();
                } catch (err) {
                    console.error('Error deleting passkey:', err);
                    showDashboardMessage(err.message || 'Could not delete passkey.', true);
                }
            }

            if (addPasskeyButton) {
                addPasskeyButton.addEventListener('click', async () => {
                    if (!csrfDetails) return;

                    const friendlyName = prompt("Enter a friendly name for this new passkey (e.g., 'My Laptop Fingerprint'):");
                    if (!friendlyName || friendlyName.trim() === "") {
                        showDashboardMessage('Passkey registration cancelled: Friendly name is required.', true);
                        return;
                    }

                    try {
                        // 1. Get registration options from server
                        const optionsResponse = await fetch('/api/profile/passkeys/registration-options', {
                            method: 'POST',
                            headers: {
                                // 'Content-Type': 'application/json', // Not strictly needed for empty body POST
                                [csrfDetails.headerName]: csrfDetails.token
                            }
                        });

                        if (!optionsResponse.ok) {
                            const errorData = await optionsResponse.json().catch(() => ({message: 'Failed to parse error response from options endpoint.'}));

                            throw new Error(errorData.message || 'Failed to get passkey registration options: ' + optionsResponse.status + ' ' + optionsResponse.statusText);
                        }
                        const options = await optionsResponse.json();

                        // --- HELPER FUNCTION TO GET THE STRING TO DECODE ---
                        function getStringValueForDecoding(fieldValue) {
                            if (fieldValue && typeof fieldValue === 'object' && typeof fieldValue.bytes === 'string') {
                                console.warn("Received object with .bytes property, extracting string:", fieldValue);
                                return fieldValue.bytes;
                            }
                            // If it's already a string, or null/undefined, or a non-conformant object,
                            // bufferDecode will handle it (expecting a string or throwing an error).
                            return fieldValue;
                        }

                        // --- END HELPER FUNCTION ---

                        // Decode challenge and user.id (user handle)
                        if (options.challenge) {
                            options.challenge = bufferDecode(getStringValueForDecoding(options.challenge));
                        }
                        if (options.user && options.user.id) {
                            options.user.id = bufferDecode(getStringValueForDecoding(options.user.id));
                        }
                        if (options.excludeCredentials) {
                            for (let cred of options.excludeCredentials) {
                                if (cred.id) {
                                    cred.id = bufferDecode(getStringValueForDecoding(cred.id));
                                }
                            }
                        }

                        // 2. Create credential with browser
                        const credential = await navigator.credentials.create({publicKey: options});

                        // 3. Send new credential to server
                        const registrationRequest = {
                            friendlyName: friendlyName.trim(),
                            id: credential.id,
                            rawId: bufferEncode(credential.rawId),
                            type: credential.type,
                            clientDataJSON: bufferEncode(credential.response.clientDataJSON),
                            attestationObject: bufferEncode(credential.response.attestationObject),
                            authenticatorAttachment: credential.authenticatorAttachment,
                            clientExtensionResults: credential.getClientExtensionResults()
                        };

                        const finishResponse = await fetch('/api/profile/passkeys', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json',
                                [csrfDetails.headerName]: csrfDetails.token
                            },
                            body: JSON.stringify(registrationRequest)
                        });

                        if (!finishResponse.ok) {
                            const errorData = await finishResponse.json().catch(() => ({message: 'Failed to parse error response from finish registration endpoint.'}));

                            throw new Error(errorData.message || 'Failed to save passkey: ' + finishResponse.status + ' ' + finishResponse.statusText);
                        }

                        showDashboardMessage('Passkey registered successfully!', false);
                        loadPasskeys();
                    } catch (err) {
                        console.error('Error registering passkey:', err);
                        showDashboardMessage(err.message || 'Passkey registration failed. Please try again.', true);
                    }
                });
            }

            // Initial load of passkeys
            if (csrfDetails) {
                loadPasskeys();
            }
        });
    </script>
`)
