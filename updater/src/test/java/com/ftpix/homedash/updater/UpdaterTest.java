package com.ftpix.homedash.updater;

import com.ftpix.homedash.models.Version;
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


        Version v1 = new Version("0.1.3"), v2 = new Version("0.1.4"), v3 = new Version("0.1.2"), v4 = new Version("1.0.0"), v5 = new Version("1.2.4"), v6 = new Version("0.2.0");
        Version wrongVersion = new Version("43423"), wrongVersion2 = new Version("fdafadfa");

        Assert.assertEquals(v1 + " < " + v2, -1, v1.compareTo(v2));
        Assert.assertEquals(v1 + " > " + v3, 1, v1.compareTo(v3));
        Assert.assertEquals(v1 + " = " + v1, 0, v1.compareTo(v1));
        Assert.assertEquals(v1 + " < " + v4, -1, v1.compareTo(v4));
        Assert.assertEquals(v1 + " < " + v6, -1, v1.compareTo(v6));
        Assert.assertEquals(v6 + " > " + v2, 1, v6.compareTo(v2));
        Assert.assertEquals(v5 + " > " + v6, 1, v5.compareTo(v6));
        Assert.assertEquals(v6 + " = " + v6, 0, v6.compareTo(v6));

    }
}
