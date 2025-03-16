/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.service;

import com.google.common.cache.Cache;
import com.google.common.hash.BloomFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Daniel Itiro Tikamori
 * @version 1.0
 * @since 2025-02-16
 * Probabilistic caching (e.g., using Bloom filters) can be used to reduce the number of cache misses by quickly determining whether an item is not in the cache.
 * This can be particularly useful in scenarios where cache misses are costly, such as database queries or external API calls.
 * However, probabilistic caching introduces a trade-off between accuracy and performance, as there is a small probability of false positives.
 * In this example, a Bloom filter is used to quickly determine whether an IP address is likely to be in the cache, and if so, the cache is checked for the ASN.
 * If the IP address is not in the cache, the ASN is looked up using the AsnLookupService, and the result is added to the cache and Bloom filter.
 * This approach can significantly reduce the number of cache misses and improve performance, especially in scenarios where the cache is large and the number of unique IP addresses is high.
 * However, it is important to choose an appropriate false positive rate for the Bloom filter to balance between accuracy and performance.
 * The false positive rate should be set based on the expected number of unique IP addresses and the size of the cache.
 * A lower false positive rate will result in fewer false positives, but will also require more memory to store the Bloom filter.
 * A higher false positive rate will result in more false positives, but will require less memory to store the Bloom filter.
 * The optimal false positive rate will depend on the specific use case and requirements.
 * In this example, the false positive rate is set to 0.01, which means that there is a 1% chance of a false positive.
 * This is a reasonable trade-off between accuracy and performance for many use cases.
 * However, the false positive rate can be adjusted based on the specific requirements of the application.
 * For example, if the application requires high accuracy, the false positive rate can be reduced.
 * If the application requires high performance, the false positive rate can be increased.
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class ProbabilisticAsnCacheServiceImpl implements ProbabilisticAsnCacheService {
    private final BloomFilter<String> bloomFilter;
    private final Cache<String, String> cache;
    private final AsnLookupService lookupService;

    @Override
    public String getAsn(String ip) {
        if (!bloomFilter.mightContain(ip)) {
            return lookupService.lookupAsn(ip);
        }

        String asn = cache.getIfPresent(ip);
        if (asn != null) {
            return asn;
        }

        asn = lookupService.lookupAsn(ip);
        if (asn != null) {
            cache.put(ip, asn);
            bloomFilter.mightContain(ip);
        }
        return asn;
    }
}
