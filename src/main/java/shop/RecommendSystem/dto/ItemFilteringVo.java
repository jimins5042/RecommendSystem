package shop.RecommendSystem.dto;

import lombok.*;

@Getter
@Setter
public class ItemFilteringVo {

    //vggnet을 이용한 희소 인덱싱 필터링 용
    private byte[] targetBitArray;
    private String layerList;

    // resnet을 이용한 ivf-pq 필터링 용
    private byte[] embeddingBytes;
    private String classFilter;

    public ItemFilteringVo sparseFeatureIndexing(byte[] targetBitArray, String layerList) {
        this.targetBitArray = targetBitArray;
        this.layerList = layerList;

        return this;
    }

    public ItemFilteringVo pqFiltering(byte[] embeddingBytes, String classFilter) {
        this.embeddingBytes = embeddingBytes;
        this.classFilter = classFilter;

        return this;
    }

}
