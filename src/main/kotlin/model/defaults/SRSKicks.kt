package model.defaults

enum class SRSKicks(
    val cw: List<List<Pair<Int, Int>>>,
    val ccw: List<List<Pair<Int, Int>>>,
    val _180: List<List<Pair<Int, Int>>>,
) {
    // For J, L, S, Z, T pieces
    STANDARD(
        cw = listOf(
            listOf(0 to 0, -1 to 0, -1 to 1, 0 to -2, -1 to -2), // 0->1
            listOf(0 to 0, 1 to 0, 1 to -1, 0 to 2, 1 to 2),    // 1->2
            listOf(0 to 0, 1 to 0, 1 to 1, 0 to -2, 1 to -2),    // 2->3
            listOf(0 to 0, -1 to 0, -1 to -1, 0 to 2, -1 to 2)  // 3->0
        ),
        ccw = listOf(
            listOf(0 to 0, 1 to 0, 1 to 1, 0 to -2, 1 to -2),    // 0->3
            listOf(0 to 0, 1 to 0, 1 to -1, 0 to 2, 1 to 2),    // 3->2
            listOf(0 to 0, -1 to 0, -1 to 1, 0 to -2, -1 to -2), // 2->1
            listOf(0 to 0, -1 to 0, -1 to -1, 0 to 2, -1 to 2)  // 1->0
        ),
        _180 = listOf(
            listOf(0 to 0, 0 to 1, 1 to 1, -1 to 1, -1 to 0, 1 to 0), // 0->2
            listOf(0 to 0, 1 to 0, 1 to 2, 0 to 2, 1 to 1, 0 to 1),   // 1->3
            listOf(0 to 0, 0 to -1, -1 to -1, 1 to -1, 1 to 0, -1 to 0), // 2->0
            listOf(0 to 0, -1 to 0, -1 to 2, 0 to 2, -1 to 1, 0 to 1)    // 3->1
        )
    ),

    // For the I-piece
    I_PIECE(
        cw = listOf(
            listOf(0 to 0, -2 to 0, 1 to 0, -2 to -1, 1 to 2),  // 0->1
            listOf(0 to 0, -1 to 0, 2 to 0, -1 to 2, 2 to -1),  // 1->2
            listOf(0 to 0, 2 to 0, -1 to 0, 2 to 1, -1 to -2),  // 2->3
            listOf(0 to 0, 1 to 0, -2 to 0, 1 to -2, -2 to 1)   // 3->0
        ),
        ccw = listOf(
            listOf(0 to 0, 1 to 0, -2 to 0, 1 to -2, -2 to 1),  // 0->3
            listOf(0 to 0, 2 to 0, -1 to 0, 2 to 1, -1 to -2),  // 3->2
            listOf(0 to 0, -1 to 0, 2 to 0, -1 to 2, 2 to -1),  // 2->1
            listOf(0 to 0, -2 to 0, 1 to 0, -2 to -1, 1 to 2)   // 1->0
        ),
        _180 = listOf(
            listOf(0 to 0, 0 to 1, 0 to 2, 0 to -1, 0 to -2),    // 0->2
            listOf(0 to 0, 1 to 0, 2 to 0, -1 to 0, -2 to 0),   // 1->3
            listOf(0 to 0, 0 to -1, 0 to -2, 0 to 1, 0 to 2),   // 2->0
            listOf(0 to 0, -1 to 0, -2 to 0, 1 to 0, 2 to 0)    // 3->1
        )
    );
}