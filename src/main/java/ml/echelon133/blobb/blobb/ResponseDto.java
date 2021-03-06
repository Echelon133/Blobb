package ml.echelon133.blobb.blobb;

import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

public class ResponseDto {

    @Length(min = 1, max = 300, message = "Response length is invalid")
    @NotNull(message = "Field 'content' must be provided")
    private String content;

    public ResponseDto() {}

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
