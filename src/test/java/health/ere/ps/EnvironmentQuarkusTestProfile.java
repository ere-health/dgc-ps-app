package health.ere.ps;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class EnvironmentQuarkusTestProfile implements QuarkusTestProfile {
    @Override
    public String getConfigProfile() {
        return "env_test";
    }
}
