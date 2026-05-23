package com.example.personalfinance.data

import java.util.Locale

object ExpenseCategoryClassifier {
    const val CATEGORY_FOOD_CAFE = "식비/카페"
    const val CATEGORY_LIVING_MART = "생활/마트"
    const val CATEGORY_SHOPPING_ONLINE = "쇼핑/온라인"
    const val CATEGORY_CULTURE_LEISURE = "문화/여가"
    const val CATEGORY_FIXED_SUBSCRIPTION = "고정비/구독"
    const val CATEGORY_HEALTH_MEDICAL = "건강/의료"
    const val CATEGORY_OTHER = "기타"

    val categories = listOf(
        CATEGORY_FOOD_CAFE,
        CATEGORY_LIVING_MART,
        CATEGORY_SHOPPING_ONLINE,
        CATEGORY_CULTURE_LEISURE,
        CATEGORY_FIXED_SUBSCRIPTION,
        CATEGORY_HEALTH_MEDICAL,
        CATEGORY_OTHER
    )

    private val rules = listOf(
        CATEGORY_FOOD_CAFE to listOf(
            // 카페/커피
            "스타벅스", "메가커피", "이디야", "투썸", "컴포즈커피", "빽다방",
            "할리스커피", "더벤티커피", "커피빈", "연커피", "카페바인",
            "커피", "카페",
            // 배달
            "배민", "배달의민족", "요기요", "쿠팡이츠",
            // 패스트푸드/외식 브랜드
            "맥도날드", "버거킹", "롯데리아",
            "노브랜드버거", "맘스터치", "모스버거",
            "피자", "치킨", "통닭", "교촌", "굽네", "bhc", "네네치킨",
            "도미노", "피자헛", "파파존스",
            // 식당/한식
            "식당", "한식", "분식", "가정식",
            "국밥", "설렁탕", "순댓국", "감자탕", "해장국",
            "스시", "초밥", "라멘", "우동", "소바",
            "고깃집", "삼겹살", "갈비", "곱창", "족발", "보쌈",
            "중국집", "짜장", "짬뽕", "마라탕",
            "분식점", "김밥", "떡볶이",
            // 빵/베이커리
            "제빵", "빵집", "빵소", "베이커리",
            "파리바게트", "뚜레쥬르", "파리크루아상",
            "성심당", "브레드이발소",
            // 간식/디저트
            "간식", "디저트", "아이스크림",
            "배스킨", "하겐다즈", "설빙", "빙수",
            "떡집", "한과"
        ),
        CATEGORY_LIVING_MART to listOf(
            // 편의점
            "CU", "씨유", "GS25", "지에스25", "세븐일레븐",
            "이마트24", "미니스톱", "편의점",
            // 대형마트/슈퍼
            "이마트", "홈플러스", "롯데마트", "하나로마트",
            "마트", "슈퍼", "농협하나로",
            // 생활용품
            "다이소", "버터", "노브랜드",
            // 정육/식재료
            "정육", "정육점", "축산", "식육", "한우", "육류",
            "청춘정육", "탑플러스마트", "탑플러스",
            // 세탁/청소
            "세탁", "크리닝",
            // 꽃집
            "꽃집", "플라워"
        ),
        CATEGORY_SHOPPING_ONLINE to listOf(
            // 온라인 쇼핑
            "쿠팡", "네이버페이", "네이버파이낸셜",
            "11번가", "G마켓", "옥션", "SSG", "신세계",
            "무신사", "지그재그", "에이블리", "브랜디",
            "오늘의집", "카카오선물", "카카오쇼핑",
            "롯데온", "현대hmall",
            "쇼핑", "온라인",
            // 다운로드/콘텐츠 구매
            "다우데이타"
        ),
        CATEGORY_CULTURE_LEISURE to listOf(
            // 영화
            "CGV", "롯데시네마", "메가박스",
            // OTT/스트리밍
            "넷플릭스", "티빙", "웨이브", "왓챠", "디즈니",
            "멜론", "지니뮤직", "플로", "스포티파이",
            "유튜브프리미엄",
            // 게임
            "Steam", "스팀", "플레이스테이션", "닌텐도",
            "PC방", "게임",
            // 여가/문화
            "노래방", "볼링", "당구", "탁구",
            "놀이공원", "테마파크", "워터파크",
            "박물관", "미술관", "공연", "전시",
            "독서실", "스터디카페",
            // 스포츠
            "스크린골프", "골프", "테니스", "수영",
            "크로스핏", "필라테스", "요가"
        ),
        CATEGORY_FIXED_SUBSCRIPTION to listOf(
            // 통신
            "SKT", "KT", "LGU", "LG U+", "알뜰폰",
            "통신요금", "인터넷요금",
            // 공과금/고정비
            "한국전력", "전력공사", "한전",
            "도시가스", "수도요금", "관리비",
            "자동납부", "정기결제",
            // 보험/금융
            "보험", "생명보험", "손해보험",
            // 구독
            "구독", "멤버십"
        ),
        CATEGORY_HEALTH_MEDICAL to listOf(
            // 의료기관
            "약국", "병원", "의원", "치과", "한의원",
            "정형외과", "내과", "피부과", "안과", "이비인후과",
            "산부인과", "소아과",
            // 건강/미용
            "올리브영", "랄라블라",
            "씨제이올리브", "CJ올리브",
            // 헬스
            "헬스장", "건강"
        )
    )

    fun classify(merchantName: String, rawText: String): String {
        val normalizedInput = normalize("$merchantName $rawText")
        return rules.firstOrNull { (_, keywords) ->
            keywords.any { keyword -> normalizedInput.contains(normalize(keyword)) }
        }?.first ?: CATEGORY_OTHER
    }

    private fun normalize(value: String): String =
        value.lowercase(Locale.ROOT).filterNot { it.isWhitespace() }
}
