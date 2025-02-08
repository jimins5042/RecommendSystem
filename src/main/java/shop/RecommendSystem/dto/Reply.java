package shop.RecommendSystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter@Setter
public class Reply {
    private Long replyId;
    private Long postId;
    private String replyWriter;
    private String replyContent;
    private String replyPassword;
    private Date replyDate;

    public Reply() {
    }

    public Reply(Long postId, String replyWriter, String replyContent, String replyPassword) {
        this.postId = postId;
        this.replyWriter = replyWriter;
        this.replyContent = replyContent;
        this.replyPassword = replyPassword;
    }

    public Reply(Long replyId, Long postId, String replyWriter, String replyContent, String replyPassword, Date replyDate) {
        this.replyId = replyId;
        this.postId = postId;
        this.replyWriter = replyWriter;
        this.replyContent = replyContent;
        this.replyPassword = replyPassword;
        this.replyDate = replyDate;
    }
}
