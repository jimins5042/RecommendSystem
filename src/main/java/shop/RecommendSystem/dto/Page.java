package shop.RecommendSystem.dto;


import lombok.Getter;
import lombok.Setter;

@Getter@Setter
public class Page {

    Long pageNo;
    Long totalPages;
    Long startPage;
    Long endPage;

    public Page() {
    }

    public Page(Long pageNo, Long totalPages, Long startPage, Long endPage) {
        this.pageNo = pageNo;
        this.totalPages = totalPages;
        this.startPage = startPage;
        this.endPage = endPage;
    }
}
