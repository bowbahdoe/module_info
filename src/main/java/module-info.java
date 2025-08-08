import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.mccue.module_info {
    requires org.jspecify;
    requires com.fasterxml.jackson.annotation;

    exports dev.mccue.module_info;
}