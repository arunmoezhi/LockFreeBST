
public class SeekRecord
{
	Node ancestor;
	Node successor;
	Node parent;
	Node leaf;
	
	public SeekRecord(Node ancestor, Node successor, Node parent, Node leaf)
	{
		this.ancestor = ancestor;
		this.successor = successor;
		this.parent = parent;
		this.leaf = leaf;
	}
}
