package io.kestra.core.validations.validator;

import io.kestra.core.models.flows.TaskDefault;
import io.kestra.core.models.validations.ModelValidator;
import io.kestra.core.serializers.YamlFlowParser;
import io.kestra.core.services.TaskDefaultService;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.validation.validator.constraints.ConstraintValidator;
import io.micronaut.validation.validator.constraints.ConstraintValidatorContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import io.kestra.core.validations.TaskDefaultValidation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Singleton
@Introspected
public class TaskDefaultValidator implements ConstraintValidator<TaskDefaultValidation, TaskDefault> {
    @Inject
    private ModelValidator modelValidator;

    @Inject
    private TaskDefaultService taskDefaultService;

    @Inject
    private YamlFlowParser yamlFlowParser;

    @Override
    public boolean isValid(@Nullable TaskDefault value, @NonNull AnnotationValue<TaskDefaultValidation> annotationMetadata, @NonNull ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        List<String> violations = new ArrayList<>();

        if (value.getValues() == null) {
            violations.add("Null values map found");
            context.messageTemplate("Invalid Task Default: " + String.join(", ", violations));

            return false;
        }

        // Check if the "values" map is empty
        for (Map.Entry<String, Object> entry : value.getValues().entrySet()) {
            if (entry.getValue() == null) {
                violations.add("Null value found in values with key " + entry.getKey());
            }
        }

        if (!violations.isEmpty()) {
            context.messageTemplate("Invalid Task Default: " + String.join(", ", violations));

            return false;
        } else {

            return true;
        }
    }
}
