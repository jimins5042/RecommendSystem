# 프로젝트 소개
본 프로젝트에서는 사용자가 제공한 질의 이미지와 유사한 객체가 포함된 상품을 추천하기 위해, 성능 최적화를 고려한 내용 기반 이미지 검색(Content-Based Image Retrieval, CBIR) 기법을 제안한다.
pHash와 VGG16 모델을 활용하여 추출한 고차원  특징점(Image Feature)를 간소화하고, K-NN 알고리즘을 통해 일정 수준의 정확도를 유지하면서 실행속도, 메모리 차지 용량 최적화를 진행하였다.

유사 객체 검색 기반 추천 로직
1. 특성 추출
- VGGNet 모델은 질의 데이터를 3×3 크기의 작은 필터를 여러 개 쌓아올린 convolution 레이어을 통과시키며 특징점을 추출.
- 유사한 이미지는 필터 반응 강도 또한 유사할 것이라 가정하고 로직을 구현.

2. Search data 생성
- 마지막 convolution 레이어(7×7×512)에서, 각 필터가 추출한 특징점의 평균값을 계산 후 내림차순 정렬.
- 상위 25개 필터의 Index를 기반으로 512bit 크기의 비트 벡터 생성.
- 25,088차원의 Feature 히스토그램을 평균값을 기준으로 이진화하여 메모리 사용량 절감.

3. 유사 이미지 검색
- 필터 반응이 큰 순서대로, 해당 필터 index를 포함하는 상품 번호를 검색.
- AND 비트 연산을 활용해 검색 범위를 점진적으로 축소.
- 남은 상품번호의 개수가 300개 이하이거나 25개의 필터 indes를 모두 순회한 경우, <br>
  질의 이미지와 남은 상품 이미지의 유사도를 계산

<br>

유사 이미지 검색 기반 추천 로직
- pHash는 이미지를 일정 크기로 리사이즈한 다음, 이산 코사인 변환(DCT)을 수행하여 64bit 해시값을 계산하는 해시 함수.
- 이미지 간의 세밀한 차이를 감지할 수 있도록 pHash의 구조를 개선하여 256bit 해시값을 산출.
- Prefix Filtering을 통해 질의 이미지의 해시값과 일부 일치하는 상품을 필터링한 다음, 해밍 거리(Hamming Distance)를 사용하여 최종 추천 상품 목록을 반환
https://github.com/jimins5042/MakeBouquetV2

개발 기록
- https://velog.io/@2jooin1207/series/%EC%9D%B4%EB%AF%B8%EC%A7%80-%EA%B2%80%EC%83%89-%EA%B8%B0%EB%B0%98-%EC%B6%94%EC%B2%9C-%EC%8B%9C%EC%8A%A4%ED%85%9C

