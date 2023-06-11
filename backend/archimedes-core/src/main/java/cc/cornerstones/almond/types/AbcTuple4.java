package cc.cornerstones.almond.types;

/**
 * @author bbottong
 */
public class AbcTuple4<F, S, T, U> {
    public F f;
    public S s;
    public T t;
    public U u;

    public AbcTuple4() {
    }

    public AbcTuple4(F f, S s, T t, U u) {
        this.f = f;
        this.s = s;
        this.t = t;
        this.u = u;
    }

    @Override
    public String toString() {
        return "Tuple3{" +
                "f=" + f +
                ", s=" + s +
                ", t=" + t +
                ", u=" + u +
                '}';
    }
}
