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

	public Node(long key, long value, boolean isInternal)
	{
		this.key = key;
		this.value = value;
		if(isInternal)
		{
			this.lChild = new AtomicStampedReference<Node>(new Node(key, value, false),0);
			this.rChild = new AtomicStampedReference<Node>(new Node(key, value, false),0);
		}
	}

	public Node(long key, long value, AtomicStampedReference<Node> lChild, AtomicStampedReference<Node> rChild)
	{
		this.key = key;
		this.value = value;
		this.lChild = lChild;
		this.rChild = rChild;
	}
}