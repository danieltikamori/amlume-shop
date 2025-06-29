/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

-- Create LTREE extension for hierarchical data
CREATE EXTENSION IF NOT EXISTS ltree;

-- Create custom operators and functions for LTREE if needed
CREATE OR REPLACE FUNCTION ltree_to_text(ltree)
    RETURNS text AS
$$
BEGIN
    RETURN $1::text;
END;
$$ LANGUAGE plpgsql IMMUTABLE
                    STRICT;

-- Create index operator class for LTREE if needed
DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1
                       FROM pg_opclass
                       WHERE opcname = 'gist_ltree_ops') THEN
            CREATE OPERATOR CLASS gist_ltree_ops
                DEFAULT FOR TYPE ltree USING gist AS
                OPERATOR 1 < ,
                OPERATOR 2 <= ,
                OPERATOR 3 = ,
                OPERATOR 4 >= ,
                OPERATOR 5 > ,
                FUNCTION 1 ltree_consistent(internal, ltree, smallint, oid, internal),
                FUNCTION 2 ltree_union(internal, internal),
                FUNCTION 3 ltree_compress(internal),
                FUNCTION 4 ltree_decompress(internal),
                FUNCTION 5 ltree_penalty(internal, internal, internal),
                FUNCTION 6 ltree_picksplit(internal, internal),
                FUNCTION 7 ltree_same(ltree, ltree, internal);
        END IF;
    END
$$;
