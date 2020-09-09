package ml.echelon133.blobb.blobb;

import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

public class ReblobbDto {

    @Length(max = 300, message = "Reblobb length is invalid")
    @NotNull(message = "Field 'content' must be provided")
    private String content;

    public ReblobbDto() {}

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
