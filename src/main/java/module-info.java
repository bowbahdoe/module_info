import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.mccue.module_info {
    requires static org.jspecify;
    requires static com.fasterxml.jackson.annotation;

    exports dev.mccue.module_info;
}