package shop.RecommendSystem.dto;


import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;

@Getter@Setter
public class Page {


    private Long pageNo;
    private Long totalPages;
    private Long startPage;
    private Long endPage;
    private Long firstPage;
    private Long finalPage;
    private String moveUrl;

    public Page() {
    }

    public Page(Long pageNo, Long totalPages, Long startPage, Long endPage) {
        this.pageNo = pageNo;
        this.totalPages = totalPages;
        this.startPage = startPage;
        this.endPage = endPage;
    }
}
