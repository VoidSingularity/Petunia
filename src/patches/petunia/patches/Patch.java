package petunia.patches;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target ({ElementType.TYPE})
@Retention (RetentionPolicy.RUNTIME)
public @interface Patch {
    /**
     * Internal name of the class you are trying to patch.
     * E.g. java/lang/Object
     * @return The name
     */
    String value ();
}
