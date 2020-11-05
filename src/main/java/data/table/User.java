package data.table;

import org.immutables.value.Value;

@Value.Immutable
public interface User {
    long getId();
    String getUsername();
    String getPassword();
}
