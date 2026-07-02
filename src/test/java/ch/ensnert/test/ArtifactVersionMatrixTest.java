package ch.ensnert.test;

import ch.ensnert.api.Index;
import ch.ensnert.api.SaveStated;
import ch.ensnert.impl.ArtifactVersionMatrix;

import java.util.HashMap;
import java.util.HashSet;


@SuppressWarnings("unused")
public class ArtifactVersionMatrixTest
{
	public void testOneEntry()
	{
		ArtifactVersionMatrix state = new ArtifactVersionMatrix();

		state.addVersion("A", "1", "B", "1");
		assert state.index.size() == 2; // A1,B1
		assert state.versions.size() == 1; // {A1,B1}@A
		assert "A".equals(state.versions.get(state.index.get(new Index("A", "1")).get(0)).get("@")) : "lookup of A1 returned wrong @";
		assert "A".equals(state.versions.get(state.index.get(new Index("B", "1")).get(0)).get("@")) : "lookup of B1 returned wrong @";
	}

	public void testTwoEntries()
	{
		ArtifactVersionMatrix state = new ArtifactVersionMatrix();
		state.addVersion("A", "1", "B", "1");

		state.addVersion("A", "2", "B", "2");
		assert state.index.size() == 4 : state.index.size(); // A1,B1,A2,B2
		assert state.versions.size() == 2; // {A1,B1}@A, {A2,B2}@A

	}

	public void testSplitBug()
	{
		ArtifactVersionMatrix state = new ArtifactVersionMatrix();
		state.addVersion("A", "1", "B", "1");
		state.addVersion("A", "2", "B", "2");
		state.addVersion("C", "1", "D", "1");
		state.addVersion("C", "2", "D", "2");
		// A1,B1,A2,B2,C1,C2,D1,D2
		// {A1,B1}@A, {A2,B2}@A, {C1,D1}@C, {C2,D2}@C

		SaveStated<ArtifactVersionMatrix> states = SaveStated.of(state);
		states.trySave("unbound");

		states.get().addVersion("D", "1", "B", "1"); // this could create a new root : @A+C
		// {A1,B1}@A, {A2,B2}@A, {C1,D1}@C, {C2,D2}@C

		states.revert("unbound");

		states.get().addVersion("B", "1", "C", "1");
		states.get().addVersion("B", "1", "D", "1");

		// states.get().printState();

	}

	/// temporarily moved the test methods into its own file.

	@SuppressWarnings({"all"}) // this method is not yet done. just ignore it!
	private static boolean isMapping(ArtifactVersionMatrix matrix, String... keys)
	{
		HashSet<Index> indexes = new HashSet<>();
		HashMap<String, String> expectanceMap = new HashMap<>();
		for (int i = 0; i < keys.length - 1; i += 2)
		{
			expectanceMap.put(keys[i], keys[i + 1]);
			if (!"@".equals(keys[i]))
				indexes.add(new Index(keys[i], keys[i + 1]));
		}
		// expectanceMap
		// matrix.index.get()
		// List<Integer> integers = matrix.index.get(lookupIdx);

		return false;

	}

	public static void testMain(String[] args)
	{
		/*
		 * Format: { source -> target } ; iX == index of node X; {version,version,version}[masterversion] // masterversion can never be not unique.
		 * Adding A->B
		 *   => (new) iA,B ->  new {A,B} [@A]
		 *    - neither A nor B exist.
		 * Adding B->C
		 *   => iA,B,C -> {A,B,C} [@A]
		 *    - index B exists as artifact, all pointers to B will add C.
		 * * * * note, this won't work if A->C and B->C2 and A->B
		 * Adding Z->A
		 *   => iZ,A,B,C -> {A,B,C,Z}[@Z]
		 *    - index A exists and @ is A, and Z->A is A a dependency, making @ to @Z
		 * Adding Y->A
		 *   => iY,Z,A,B,C -> {A,B,C,Z,Y}[@Z]
		 *    - index A exists but @ is not Y or A so Y is just added as ListEntry
		 * Adding Y2->A
		 *   => iY,Y2,Z,A,B,C -> {A,B,C,Z,Y&Y2}[@Z]
		 *    - the Y exists in index for A, so index Y2 adds a pointer to the old Y, while merging the Y field to "Y,Y2"
		 * Save State Alpha : "iY,Y2,Z,A,B,C -> {A,B,C,Z,Y&Y2}[@Z]"
		 *
		 * Special Cases:
		 * Adding A2->B
		 *   => iY,Y2,Z,A -> {A,B,C,Z,Y&Y2}[@Z] ; (new) iA2 -> {A2,B,C}[@A2] ; (extracted) iB,C -> IndexesOf{A,A2}
		 *    - why? because A* can only resolve to B and B already resolved to C; while Y,Y2 and Z can not resolve to A2
		 *    ! this can only be calculated, if it is known that B uses C but not Z or Y; which it currently does not without storing ALL relations
		 *    ! if the A was all added at once; then this issue won't arise.
		 * Adding F->B
		 *   => iY,Y2,Z,A -> {A,B,C,Z,Y&Y2}[@Z]; iA2 -> {A2,B,C}[@A2}; (new) iF -> {F,B,C}[@F]; iB,C -> IndexOf {A,A2,F}
		 *    ! again, like above. this can only be calculated if it is known, that B uses C but not Z or Y.
		 * * * * * * * * *
		 * Reset and Load State Alpha ; "iY,Y2,Z,A,B,C -> {A,B,C,Z,Y&Y2}[@Z]"
		 * Adding R->Q
		 *   => iY,Y2,Z,A,B,C -> {A,B,C,Z,Y&Y2}[@Z] ; (new) iR,Q -> {R,Q}[@R]
		 *    - neiter R or Q is indexed, add them as unique entry
		 * Adding R2->Q
		 *   => iY,Y2,Z,A,B,C -> {A,B,C,Z,Y&Y2}[@Z] ; (mod) iR -> {R,Q}[@R] ; (new)iR2 -> {R2,Q}[@R2] ; (expanded)iQ -> IndexOf {R,R2}
		 *    - Q is indexed; R* exists in index Q; R is higher and @ is R so the complete entry is copied and R is overwritten with R2, while the index of all keys (Q) are updated to also use the new R2
		 * Save State "Bravo" : "iY,Y2,Z,A,B,C -> {A,B,C,Z,Y&Y2}[@Z] ; iR -> {R,Q}[@R] ; iR2 -> {R2,Q}[@R2] ; iQ -> IndexOf {R,R2}"
		 * * * * * * * * *
		 * if Adding A->B
		 *   => (no modification)
		 *    - A and B are indexed, since A is higher, Entry for A is checked for B - beeing the same, will not change anything
		 * if Adding Q->A (1 probably best with ooption of 3)
		 *  1=> iR -> {A,B,C,Z,Y&Y2,Q,R}[@R]; iR2 -> {A,B,C,Z,Y&Y2,Q,R2}[@R2]; iZ,Y,Y2,A,B,C,Q -> IndexOf {R,R2} // R as Master
		 *  2=> iZ,Y,Y2,A,B,C,Q,R,R2 -> {A,B,C,Z,Y&Y2,Q,R&R2}[@Z] // Z as Master
		 *  3=> iR -> {A,B,C,Z,Y&Y2,Q,R}[@R,Z]; iR2 -> {A,B,C,Z,Y&Y2,Q,R2}[@R2,Z]; iZ,Y,Y2,A,B,C,Q -> IndexOf {R,R2} // R* and Z* as Master
		 *        !3: making R and Z Masters means Idx(R)*Idx(Z) entries : R R2 R3 * Z Z2 Z3 -> (R,Z) (R,Z2) (R,Z3) (R2,Z) (R2,Z2) (R2,Z3) (R3,Z) (R3,Z2) (R3,Z3)
		 *        !3: this could be wanted; if you search for all versions of Z* by R2 and A it will give all Z,Z2,Z3 you wanted
		 *    - Q and A are indexed, since Q is higher, Entry for Q is checked for A - failing.
		 *    - > 1:it failing will check if Entry for A has @ as A, also failing will choose @ of Q as index merge @ of Entries of Q (@R,@R2) to the @ of Entries of A (@Z)
		 *    - > 2:it failing will check if Entry for A has @ as A, also failing will choose @ of A as index merge @ of Entries of Q (@R,@R2) to the @ of Entries of A (@Z)
		 *    - > 3:it failing will check if Entry for A has @ as A, also failing will merge the @ of Q and A as index merge @ of Entries of Q (@R,@R2) with the @ of Entries of A (@Z)
		 *    - R and Z can be masters, if adding to any R or Z just one of them is Updated from Master ; if you add to neither R or Z, the entry is just added as List
		 * if Adding A->Q (2 probably best, with option of 3)
		 *  1=> iR -> {A,B,C,Z,Y&Y2,Q,R}[@R]; iR2 -> {A,B,C,Z,Y&Y2,Q,R2}[@R2]; iZ,Y,Y2,A,B,C,Q -> IndexOf {R,R2} // R,R2 as Master
		 *  2=> iZ,Y,Y2,A,B,C,Q,R,R2 -> {A,B,C,Z,Y&Y2,Q,R&R2}[@Z] // Z as Master
		 *  3=> iR -> {A,B,C,Z,Y&Y2,Q,R}[@R,Z]; iR2 -> {A,B,C,Z,Y&Y2,Q,R2}[@R2,Z]; iZ,Y,Y2,A,B,C,Q -> IndexOf {R,R2} // R* and Z* as Master
		 *    - Q and A are indexed, since A is higher, Entry for A is checked for Q - failing.
		 *    - > 1:it failing will check if Entry for Q has @ as Q, also failing will choose @ of Q as index merge @ of Entries of A (@Z) to the @ of Entries of Q (@R,@R2)
		 *    - > 2:it failing will check if Entry for Q has @ as Q, also failing will choose @ of A as index merge @ of Entries of A (@Z) to the @ of Entries of Q (@R,@R2)
		 *    - > 3:it failing will check if Entry for A has @ as A, also failing will merge the @ of Q and A as index merge @ of Entries of Q (@R,@R2) with the @ of Entries of A (@Z)
		 * * * * * * * * *
		 * */

		ArtifactVersionMatrix matrix = new ArtifactVersionMatrix();
		SaveStated<ArtifactVersionMatrix> ss = SaveStated.of(matrix);

		ss.get().addVersion("A", "1", "B", "1"); /// => {A,B}@A
		ss.get().addVersion("B", "1", "C", "1"); /// => {A,B,C}@A
		ss.get().addVersion("Z", "1", "A", "1"); /// => {Z,A,B,C}@Z
		ss.get().addVersion("Y", "1", "A", "1"); /// => {Z,Y,A,B,C}@Z
		ss.get().addVersion("Y", "2", "A", "1"); /// => {Z,Y&Y2,A,B,C}@Z
		ss.trySave("alpha").get().printState();

		assert ss.get().index.size() == 6; // A,B,C,Y,Y2,Z
		assert ss.get().versions.size() == 1; // {A,B,C,Y&Y2,Z}
		// worked

		ss.get().addVersion("A", "2", "B", "1"); /// => {A&A2,B,C,Z,Y&Y2}@Z;
		ss.get().addVersion("F", "1", "B", "1"); /// => {A&A2,B,C,Z,Y&Y2,F}@Z;
		ss.get().printState();

		assert ss.get().index.size() == 8; // A,A2,B,F,C,Y,Y2,Z
		assert ss.get().versions.size() == 1; // {A&A2,B,C,Z,Y&Y2,F}
		// worked

		ss.revert("alpha");
		ss.get().addVersion("R", "1", "Q", "1"); /// => {Z,Y&Y2,A,B,C}@Z, {R,Q}@R
		ss.get().addVersion("R", "2", "Q", "1"); /// => {Z,Y&Y2,A,B,C}@Z, {R,Q}@R, {R2,Q}@R
		ss.trySave("beta").get().printState();

		assert ss.get().index.size() == 9; // A,B,C,R,R2,Q,Y,Y2,Z
		assert ss.get().versions.size() == 3; // {Z,Y&Y2,A,B,C}, {R,Q}, {R2,Q}
		// worked

		ss.get().addVersion("A", "1", "B", "1"); /// => {Z,Y&Y2,A,B,C}@Z, {R,Q}@R, {R2,Q}@R (no changes)
		ss.get().printState();
		assert ss.get().index.size() == 9; // A,B,C,R,R2,Q,Y,Y2,Z
		assert ss.get().versions.size() == 3; // {Z,Y&Y2,A,B,C}, {R,Q}, {R2,Q}

		ss.revert("beta");
		ss.get().addVersion("Q", "1", "A", "1"); /// => {Z,Y&Y2,A,B,C,R,Q}@Z+R, {Z,Y&Y2,A,B,C,R2,Q}@Z+R
		ss.get().printState();
		assert ss.get().index.size() == 9; // A,B,C,R,R2,Q,Y,Y2,Z
		assert ss.get().versions.size() == 2; // {A,B,C,R,Q,Y&Y2,Z}, {A,B,C,R2,Q,Y&Y2,Z}

		ss.revert("beta");
		ss.get().addVersion("A", "1", "Q", "1"); /// => {Z,Y&Y2,A,B,C,Q,R1}@Z+R, {Z,Y&Y2,A,B,C,Q,R2}@Z+R,
		ss.get().printState();
		assert ss.get().index.size() == 9; // A,B,C,R1,R2,Q,Y,Y2,Z
		assert ss.get().versions.size() == 2; // {A,B,C,R1,Q,Y&Y2,Z}, {A,B,C,R2,Q,Y&Y2,Z}

		ss.dropAll();
	}

	public static void main2(String[] args)
	{
		ArtifactVersionMatrix matrix = new ArtifactVersionMatrix();
		SaveStated<ArtifactVersionMatrix> ss = SaveStated.of(matrix);

		ss.get().addVersion("A", "1", "B", "1"); /// => {A1,B1}@A

		ss.get().addVersion("A", "2", "B", "1"); /// => {A1,B1}@A, {A2,B1}@A
		ss.get().printState();

		ss.get().addVersion("A", "3", "B", "1"); /// => {A1,B1}@A, {A2,B1}@A, {A3,B1}@A
		ss.get().printState(); // wrong! makes 4 entries!
		// if all containt A; just create 1 new entry using {A,B}@A ;if there is 1 instance not having an A; set it to that A

		ss.get().addVersion("B", "1", "C", "1"); /// => {A1,B1,C1}@A, {A2,B1,C1}@A, {A3,B1,C1}@A
		ss.get().printState();

		// ss.get().addVersion("B", "1", "C", "1"); /// => {A1,B1,C1}@A, {A2,B1,C1}@A, {A3,B1,C1}@A

		ss.dropAll();
	}

}
