package health.ere.ps;

import io.quarkus.test.junit.QuarkusTestProfile;

public class EnvironmentQuarkusTestProfile implements QuarkusTestProfile {
    @Override
    public String getConfigProfile() {
        return "env_test";
    }
}
