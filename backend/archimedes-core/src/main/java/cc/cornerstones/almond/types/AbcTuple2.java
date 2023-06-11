package cc.cornerstones.almond.types;

/**
 * @author bbottong
 */
public class AbcTuple2<F, S> {
    public F f;
    public S s;

    public AbcTuple2() {
    }

    public AbcTuple2(F f, S s) {
        this.f = f;
        this.s = s;
    }

    @Override
    public String toString() {
        return "AbcTuple2{" +
                "f=" + f +
                ", s=" + s +
                '}';
    }
}
