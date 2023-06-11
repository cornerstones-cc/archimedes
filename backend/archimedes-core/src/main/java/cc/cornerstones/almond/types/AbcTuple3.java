package cc.cornerstones.almond.types;

/**
 * @author bbottong
 */
public class AbcTuple3<F, S, T> {
    public F f;
    public S s;
    public T t;

    public AbcTuple3() {
    }

    public AbcTuple3(F f, S s, T t) {
        this.f = f;
        this.s = s;
        this.t = t;
    }

    @Override
    public String toString() {
        return "Tuple3{" +
                "f=" + f +
                ", s=" + s +
                ", t=" + t +
                '}';
    }
}
