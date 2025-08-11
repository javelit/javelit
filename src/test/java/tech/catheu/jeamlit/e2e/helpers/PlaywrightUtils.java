package tech.catheu.jeamlit.e2e.helpers;

import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.assertions.LocatorAssertions;

public class PlaywrightUtils {

    public static final LocatorAssertions.IsVisibleOptions WAIT_1_SEC_MAX = new LocatorAssertions.IsVisibleOptions().setTimeout(1000);
    public static final LocatorAssertions.IsVisibleOptions WAIT_100_MS_MAX = new LocatorAssertions.IsVisibleOptions().setTimeout(100);
    public static final BrowserType.LaunchOptions HEADLESS = new BrowserType.LaunchOptions().setHeadless(true);
    public static final BrowserType.LaunchOptions NOT_HEADLESS = new BrowserType.LaunchOptions().setHeadless(false);
}
