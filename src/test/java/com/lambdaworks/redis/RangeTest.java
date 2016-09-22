package com.lambdaworks.redis;

import static com.lambdaworks.redis.Range.Boundary.excluding;
import static com.lambdaworks.redis.Range.Boundary.including;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * @author Mark Paluch
 */
public class RangeTest {

    @Test
    public void unbounded() {

        Range<Object> unbounded = Range.unbounded();

        assertThat(unbounded.getLower().isIncluding()).isTrue();
        assertThat(unbounded.getLower().getValue()).isNull();
        assertThat(unbounded.getUpper().isIncluding()).isTrue();
        assertThat(unbounded.getUpper().getValue()).isNull();
    }

    @Test
    public void createIncluded() {

        Range<Object> range = Range.create("ze", "ro");

        assertThat(range.getLower().isIncluding()).isTrue();
        assertThat(range.getLower().getValue()).isEqualTo("ze");
        assertThat(range.getUpper().isIncluding()).isTrue();
        assertThat(range.getUpper().getValue()).isEqualTo("ro");
    }

    @Test
    public void fromBoundaries() {

        Range<Object> range = Range.from(including("ze"), excluding("ro"));

        assertThat(range.getLower().isIncluding()).isTrue();
        assertThat(range.getLower().getValue()).isEqualTo("ze");
        assertThat(range.getUpper().isIncluding()).isFalse();
        assertThat(range.getUpper().getValue()).isEqualTo("ro");
    }

    @Test
    public void greater() {

        Range<Object> gt = Range.unbounded().gt("zero");

        assertThat(gt.getLower().isIncluding()).isFalse();
        assertThat(gt.getLower().getValue()).isEqualTo("zero");
        assertThat(gt.getUpper().isIncluding()).isTrue();
        assertThat(gt.getUpper().getValue()).isNull();
    }

    @Test
    public void greaterOrEquals() {

        Range<Object> gte = Range.unbounded().gte("zero");

        assertThat(gte.getLower().isIncluding()).isTrue();
        assertThat(gte.getLower().getValue()).isEqualTo("zero");
        assertThat(gte.getUpper().isIncluding()).isTrue();
        assertThat(gte.getUpper().getValue()).isNull();
    }

    @Test
    public void less() {

        Range<Object> lt = Range.unbounded().lt("zero");

        assertThat(lt.getLower().isIncluding()).isTrue();
        assertThat(lt.getLower().getValue()).isNull();
        assertThat(lt.getUpper().isIncluding()).isFalse();
        assertThat(lt.getUpper().getValue()).isEqualTo("zero");
        assertThat(lt.toString()).isEqualTo("Range [[unbounded] to (zero]");
    }

    @Test
    public void lessOrEquals() {

        Range<Object> lte = Range.unbounded().lte("zero");

        assertThat(lte.getLower().isIncluding()).isTrue();
        assertThat(lte.getLower().getValue()).isNull();
        assertThat(lte.getUpper().isIncluding()).isTrue();
        assertThat(lte.getUpper().getValue()).isEqualTo("zero");
        assertThat(lte.toString()).isEqualTo("Range [[unbounded] to [zero]");
    }
}
