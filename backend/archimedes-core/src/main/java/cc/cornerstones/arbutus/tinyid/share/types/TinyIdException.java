package cc.cornerstones.arbutus.tinyid.share.types;

public class TinyIdException extends RuntimeException {

    public TinyIdException() {
        super();
    }

    public TinyIdException(String message) {
        super(message);
    }

    public TinyIdException(String message, Throwable cause) {
        super(message, cause);
    }

    public TinyIdException(Throwable cause) {
        super(cause);
    }
}
