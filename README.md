# 프로젝트 소개
본 프로젝트에서는 생성형 AI의 산출물을 상업적 목적으로 활용하고자, 내용 기반 이미지 검색(Content-Based Image Retrieval, CBIR) 기술을 활용하여 사용자가 제공한 질의 이미지와 유사한 객체가 포함된 상품을 추천하는 기법을 제안한다.

유사 객체 검색 기반 추천 로직
- ORB 라이브러리를 이용한 Image Feature 추출
- 추출한 Image Feature의 descripter를 평균값을 기준으로 이진화
- Binaryization된 Image Feature를 Min-hash를 이용해 search data를 생성(적절한 길이 테스트 중)
- LSH를 이용해 검색 (버킷 수 테스트 중)

유사 이미지 검색 기반 추천 로직
- 이미지 간의 세밀한 차이를 감지할 수 있도록 pHash의 구조를 개선
- 개선한 pHash와 Color Thief 라이브러리를 활용하여 이미지의 고유 정보를 추출 후 저장
- Prefix Filtering을 통해 질의 이미지의 해시값과 일부 일치하는 상품을 필터링한 다음, 해밍 거리(Hamming Distance)와 자카드 유사도를 사용하여 최종 추천 상품 목록을 반환
- 더 자세한 내용은 아래 링크 참고 <br>
https://github.com/jimins5042/MakeBouquetV2

개발 기록
- https://velog.io/@2jooin1207/series/%EC%9D%B4%EB%AF%B8%EC%A7%80-%EA%B2%80%EC%83%89-%EA%B8%B0%EB%B0%98-%EC%B6%94%EC%B2%9C-%EC%8B%9C%EC%8A%A4%ED%85%9C

