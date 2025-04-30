/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

// ... (other code)

function getDeviceFingerprint() {
    return {
        userAgent: navigator.userAgent,
        screenWidth: window.screen.width.toString(),
        screenHeight: window.screen.height.toString()
    };
}

// ... (in your login function)

const fingerprintData = getDeviceFingerprint();

fetch('/api/login', { // Or your login endpoint
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        username: '...',
        password: '...',
        userAgent: fingerprintData.userAgent,  // Send User-Agent
        screenWidth: fingerprintData.screenWidth, // Send screen width
        screenHeight: fingerprintData.screenHeight, // Send screen height
        // ... other data
    })
});