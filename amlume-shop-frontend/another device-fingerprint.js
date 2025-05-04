/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

function getDeviceFingerprint() {
  const userAgent = navigator.userAgent;
  const screenWidth = window.screen.width;
  const screenHeight = window.screen.height;
  // ... other data collection ...

  const fingerprintString = `${userAgent}-${screenWidth}-${screenHeight}`; // Combine data

  // Hash the fingerprint for security.  Don't send the raw string to the server.
  const hashedFingerprint = sha256(fingerprintString); // Use a suitable hashing library

  return hashedFingerprint;
}

// Example usage:
const deviceFingerprint = getDeviceFingerprint();

// Send the hashed fingerprint to your Spring Boot backend (e.g., in the login request).
fetch('/api/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    username: '...',
    password: '...',
    deviceFingerprint: deviceFingerprint, // Send the hashed fingerprint
  }),
});

// Include a suitable hashing library (e.g., js-sha256).
// You can use a CDN or install it with npm/yarn.
// Example CDN usage:
// <script src="https://cdnjs.cloudflare.com/ajax/libs/js-sha256/0.9.0/sha256.min.js"></script>