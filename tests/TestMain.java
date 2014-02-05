/**
 * Give a description of TestMain here.
 *
 * @author Freek Alleman <freek.alleman@raaftech.com>
 * @version 0.1
 * @since 2014-02-04
 */

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openpcf.universal.Main;

public class TestMain {

    private Main testMain;

    @Before
    public void setupTestEnvironment(){
        System.out.println("Setting up test environment");
        testMain = new Main();
        System.out.println("Main " + testMain + " created.");
    }

    // . does app exist?
    @Test
    public void appExistsAndIsCommandLineAppInstance(){
        Assert.assertTrue(testMain instanceof Main);
    }
    @Test
    public void appExistsAndIsCommandLineAppInstancePrecize(){
        Assert.assertEquals(Main.class, testMain.getClass());
    }

    // . does app have version() function?
    @Test
    public void appHasVersion(){
        Assert.assertEquals(testMain.getVersion(), "0.1");
    }


}
