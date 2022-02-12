package extension;

import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;

public class InsidiousRunnerSettings implements RunnerSettings {
    @Override
    public void readExternal(Element element) throws InvalidDataException {
        new Exception().printStackTrace();

    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        new Exception().printStackTrace();

    }
}
