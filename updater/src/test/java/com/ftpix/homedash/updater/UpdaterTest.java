package com.ftpix.homedash.updater;

import com.ftpix.homedash.updater.exceptions.WrongVersionPatternException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.JUnit4;

/**
 * Created by gz on 4/1/17.
 */
public class UpdaterTest {


    @Test
    public void testVersionComparison() throws WrongVersionPatternException {

        Updater updater = new Updater();

        String v1 = "0.1.3", v2 = "0.1.4", v3 = "0.1.2", v4 = "1.0.0", v5 = "1.2.4", v6 = "0.2.0";
        String wrongVersion = "43423", wrongVersion2 = "fdafadfa";

        Assert.assertEquals(v1 + " < " + v2, -1, updater.compareVersion(v1, v2));
        Assert.assertEquals(v1 + " > " + v3, 1, updater.compareVersion(v1, v3));
        Assert.assertEquals(v1 + " = " + v1, 0, updater.compareVersion(v1, v1));
        Assert.assertEquals(v1 + " < " + v4, -1, updater.compareVersion(v1, v4));
        Assert.assertEquals(v1 + " < " + v6, -1, updater.compareVersion(v1, v6));
        Assert.assertEquals(v6 + " > " + v2, 1, updater.compareVersion(v6, v2));
        Assert.assertEquals(v5 + " > " + v6, 1, updater.compareVersion(v5, v6));
        Assert.assertEquals(v6 + " = " + v6, 0, updater.compareVersion(v6, v6));

        try {
            updater.compareVersion(v1, wrongVersion);
            updater.compareVersion(wrongVersion2, v1);

            updater.compareVersion(wrongVersion, wrongVersion2);
            Assert.fail("Version should be wrong");
        } catch (WrongVersionPatternException e) {

        }
    }
}
