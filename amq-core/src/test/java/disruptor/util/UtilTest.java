/*
 * Copyright 2011 LMAX Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package disruptor.util;

import com.artlongs.amq.disruptor.Sequence;
import com.artlongs.amq.disruptor.util.Util;
import org.junit.Assert;
import org.junit.Test;

public final class UtilTest
{
    @Test
    public void shouldReturnNextPowerOfTwo()
    {
        int powerOfTwo = Util.ceilingNextPowerOfTwo(1000);

        Assert.assertEquals(1024, powerOfTwo);
    }

    @Test
    public void shouldReturnExactPowerOfTwo()
    {
        int powerOfTwo = Util.ceilingNextPowerOfTwo(1024);

        Assert.assertEquals(1024, powerOfTwo);
    }

    @Test
    public void shouldReturnMinimumSequence()
    {
        final Sequence[] sequences = {new Sequence(7L), new Sequence(3L), new Sequence(12L)};
        Assert.assertEquals(3L, Util.getMinimumSequence(sequences));
    }

    @Test
    public void shouldReturnLongMaxWhenNoEventProcessors()
    {
        final Sequence[] sequences = new Sequence[0];

        Assert.assertEquals(Long.MAX_VALUE, Util.getMinimumSequence(sequences));
    }
}
