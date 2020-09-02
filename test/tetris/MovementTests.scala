package tetris

class MovementTests extends TetrisTestSuiteBase {

  test("testMovement") {
    checkGame( List(TestFrame(4,
      """..SS..
        |.SS...
        |......
        |......"""),
      TestFrame(4, List(Left),
        """.SS...
          |SS....
          |......
          |......"""),
      TestFrame(4, List(Right),
        """..SS..
          |.SS...
          |......
          |......"""),
      TestFrame(4, List(Right),
        """...SS.
          |..SS..
          |......
          |......"""),
      TestFrame(4, List(Right),
        """....SS
          |...SS.
          |......
          |......"""),
      TestFrame(4, List(Down),
        """......
          |....SS
          |...SS.
          |......"""),
      TestFrame(4, List(Left),
        """......
          |...SS.
          |..SS..
          |......"""),
      TestFrame(4, List(Left),
        """......
          |..SS..
          |.SS...
          |......"""),
      TestFrame(4, List(Left),
        """......
          |.SS...
          |SS....
          |......"""),
      TestFrame(4, List(Down),
        """......
          |......
          |.SS...
          |SS...."""),
      TestFrame(4, List(Right),
        """......
          |......
          |..SS..
          |.SS..."""),
      TestFrame(4, List(Right),
        """......
          |......
          |...SS.
          |..SS.."""),
      TestFrame(4, List(Right),
        """......
          |......
          |....SS
          |...SS."""))
    ,hint = "Moving left, right and down should work as expected")
  }

  test("testMovement2") {
    checkGame( List(TestFrame(5,
      """...T...
        |..TTT..
        |.......
        |......."""),
      TestFrame(5, List(Left),
        """..T....
          |.TTT...
          |.......
          |......."""),
      TestFrame(5, List(Left),
        """.T.....
          |TTT....
          |.......
          |......."""),
      TestFrame(5, List(Down),
        """.......
          |.T.....
          |TTT....
          |......."""),
      TestFrame(5, List(Right),
        """.......
          |..T....
          |.TTT...
          |......."""),
      TestFrame(5, List(Right),
        """.......
          |...T...
          |..TTT..
          |......."""),
      TestFrame(5, List(Right),
        """.......
          |....T..
          |...TTT.
          |......."""),
      TestFrame(5, List(Right),
        """.......
          |.....T.
          |....TTT
          |......."""),
      TestFrame(5, List(Down),
        """.......
          |.......
          |.....T.
          |....TTT"""),
      TestFrame(5, List(Left),
        """.......
          |.......
          |....T..
          |...TTT."""),
      TestFrame(5, List(Left),
        """.......
          |.......
          |...T...
          |..TTT.."""),
      TestFrame(5, List(Left),
        """.......
          |.......
          |..T....
          |.TTT..."""),
      TestFrame(5, List(Left),
        """.......
          |.......
          |.T.....
          |TTT...."""))
      ,hint = "Moving left, right and down should work as expected")
  }

  test("testDrop") {
    checkGame( initialBoard =
      """....
        |....
        |....
        |....
        |.T..
        |TTT.""",
      List( TestFrame(0, List(Drop),
        """....
          |IIII
          |....
          |....
          |.T..
          |TTT."""),
        TestFrame(0, List(RotateRight),
          """..I.
            |..I.
            |..I.
            |..I.
            |.T..
            |TTT."""),
        TestFrame(0, List(Right),
          """...I
            |...I
            |...I
            |...I
            |.T..
            |TTT."""),
        TestFrame(5, List(Drop),
          """.T..
            |TTT.
            |....
            |...I
            |...I
            |.T.I"""),
        TestFrame(5, List(RotateRight),
          """.T..
            |.TT.
            |.T..
            |...I
            |...I
            |.T.I"""),
        TestFrame(5, List(Left),
          """T...
            |TT..
            |T...
            |...I
            |...I
            |.T.I"""),
        TestFrame(0, List(Drop),
          """....
            |IIII
            |....
            |T..I
            |TT.I
            |TT.I"""),
        TestFrame(0, List(RotateRight),
          """..I.
            |..I.
            |..I.
            |T.II
            |TT.I
            |TT.I"""),
        TestFrame(5, List(Drop),
          """.T..
            |TTT.
            |....
            |....
            |..I.
            |T.II"""),
        TestFrame(5, List(RotateRight),
          """.T..
            |.TT.
            |.T..
            |....
            |..I.
            |T.II"""),
        TestFrame(5, List(RotateRight),
          """....
            |TTT.
            |.T..
            |....
            |..I.
            |T.II"""),
        TestFrame(5, List(RotateRight),
          """.T..
            |TT..
            |.T..
            |....
            |..I.
            |T.II"""),
        TestFrame(0, List(Drop),
          """....
            |IIII
            |....
            |....
            |.T..
            |TTI."""),
        TestFrame(0, List(RotateRight),
          """..I.
            |..I.
            |..I.
            |..I.
            |.T..
            |TTI."""),
        TestFrame(0, List(Right),
          """...I
            |...I
            |...I
            |...I
            |.T..
            |TTI."""),
        TestFrame(5, List(Drop),
          """.T..
            |TTT.
            |....
            |...I
            |...I
            |.T.I"""),
        TestFrame(5, List(RotateRight),
          """.T..
            |.TT.
            |.T..
            |...I
            |...I
            |.T.I"""),
        TestFrame(5, List(RotateRight),
          """....
            |TTT.
            |.T..
            |...I
            |...I
            |.T.I"""),
        TestFrame(5, List(RotateRight),
          """.T..
            |TT..
            |.T..
            |...I
            |...I
            |.T.I"""),
        TestFrame(5, List(Right),
          """..T.
            |.TT.
            |..T.
            |...I
            |...I
            |.T.I"""),
        TestFrame(0, List(Drop),
          """....
            |IIII
            |....
            |..TI
            |.TTI
            |.TTI""")),
      hint = "The drop action immediately moves the current tetromino all the way down such that it collides with other blocks or the bottom of the board, and spawns a new tetromino."
    )
  }


}
