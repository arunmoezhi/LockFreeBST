import java.util.concurrent.atomic.AtomicStampedReference;

public class Node
{
	long key;
	long value;
	volatile AtomicStampedReference<Node> lChild;
	volatile AtomicStampedReference<Node> rChild;
	
	public Node()
	{		
	}	

	public Node(long key, long value)
	{
		this.key = key;
		this.value = value;
	}

	public Node(long key, long value, AtomicStampedReference<Node> lChild, AtomicStampedReference<Node> rChild)
	{
		this.key = key;
		this.value = value;
		this.lChild = lChild;
		this.rChild = rChild;
	}
}