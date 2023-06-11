package cc.cornerstones.biz.distributedjob.service.assembly;

public abstract class JobOperator {

    /**
     * execute, invoked when executor receives a scheduling request
     *
     * @throws Exception
     */
    public abstract void execute(Object... params) throws Exception;

    /**
     * init, invoked when JobThread init
     */
    public void init() throws Exception {
        // do something
    }


    /**
     * destroy, invoked when JobThread destroy
     */
    public void destroy() throws Exception {
        // do something
    }

}
