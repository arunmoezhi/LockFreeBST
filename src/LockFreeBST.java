import java.util.concurrent.atomic.AtomicStampedReference;

public class LockFreeBST
{
	static Node grandParentHead;
	static Node parentHead;
	static LockFreeBST obj;
	static long nodeCount=0;

	//	static final AtomicReferenceFieldUpdater<Node, Node> lUpdate = AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "lChild");
	//	static final AtomicReferenceFieldUpdater<Node, Node> rUpdate = AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "rChild");


	public LockFreeBST()
	{
	}

	public final long lookup(long target)
	{
		Node node = grandParentHead;
		while(node.lChild != null) //loop until a leaf or dummy node is reached
		{
			if(target < node.key)
			{
				node = node.lChild.getReference();
			}
			else
			{
				node = node.rChild.getReference();
			}
		}

		if(target == node.key)
		{
			//key found
			return(1);
		}
		else
		{
			//key not found
			return(0);
		}
	}

	public final void insert(long insertKey, long insertValue)
	{
		int nthChild;
		Node node;
		Node pnode;
		SeekRecord s;
		while(true)
		{
			nthChild=-1;
			pnode = parentHead;
			node = parentHead.lChild.getReference();
			while(node.lChild != null) //loop until a leaf or dummy node is reached
			{
				if(insertKey < node.key)
				{
					pnode = node;
					node = node.lChild.getReference();
				}
				else
				{
					pnode = node;
					node = node.rChild.getReference();
				}
			}

			Node oldChild = node;

			if(insertKey < pnode.key)
			{
				nthChild = 0;
			}
			else
			{
				nthChild = 1;
			}

			//leaf node is reached
			if(node.key == insertKey)
			{
				//key is already present in tree. So return
				return;
			}

			Node internalNode, lLeafNode,rLeafNode;
			if(node.key < insertKey)
			{
				//lLeafNode = new Node(node.key, node.value);
				rLeafNode = new Node(insertKey, insertValue);
				internalNode = new Node(insertKey, insertValue, new AtomicStampedReference<Node>(node,0),new AtomicStampedReference<Node>(rLeafNode,0));
			}
			else
			{
				lLeafNode = new Node(insertKey, insertValue);
				//rLeafNode =new Node(node.key, node.value);
				internalNode = new Node(node.key, node.value, new AtomicStampedReference<Node>(lLeafNode,0),new AtomicStampedReference<Node>(node,0));
			}

			if(nthChild ==0)
			{
				//if(lUpdate.compareAndSet(pnode, oldChild, cr))
				if(pnode.lChild.compareAndSet(oldChild, internalNode, 0, 0))
				{
					//System.out.println("I3 " + insertKey);
					return;
				}
				else
				{
					//insert failed; help the conflicting delete operation
					if(node == pnode.lChild.getReference()) // address has not changed. So CAS would have failed coz of flag/mark only
					{
						//help other thread with cleanup
						s = seek(insertKey);
						cleanUp(insertKey,s);
					}
				}
			}
			else
			{
				//if(rUpdate.compareAndSet(pnode, oldChild, cr))
				if(pnode.rChild.compareAndSet(oldChild, internalNode, 0, 0))
				{
					//System.out.println("I4 " + insertKey);
					return;
				}
				else
				{
					//insert failed; help the conflicting delete operation
					if(node == pnode.rChild.getReference()) // address has not changed. So CAS would have failed coz of flag/mark only
					{
						//help other thread with cleanup
						s = seek(insertKey);
						cleanUp(insertKey,s);
					}
				}
			}
		}
	}

	public final void delete(long deleteKey)
	{
		boolean isCleanUp=false;
		SeekRecord s;
		Node parent;
		Node leaf=null;
		while(true)
		{
			s = seek(deleteKey);
			if(!isCleanUp)
			{
				leaf = s.leaf;
				if(leaf.key != deleteKey)
				{
					return;
				}
				else
				{
					parent = s.parent;
					if(deleteKey < parent.key)
					{
						if(parent.lChild.compareAndSet(leaf, leaf, 0, 2)) //00 to 10 - just flag
						{
							isCleanUp = true;
							//do cleanup
							if(cleanUp(deleteKey,s))
							{
								return;
							}
						}
						else
						{
							if(leaf == parent.lChild.getReference()) // address has not changed. So CAS would have failed coz of flag/mark only
							{
								//help other thread with cleanup
								cleanUp(deleteKey,s);
							}
						}
					}
					else
					{
						if(parent.rChild.compareAndSet(leaf, leaf, 0, 2)) //00 to 10 - just flag
						{
							isCleanUp = true;
							//do cleanup
							if(cleanUp(deleteKey,s))
							{
								return;
							}
						}
						else
						{
							if(leaf == parent.rChild.getReference()) // address has not changed. So CAS would have failed coz of flag/mark only
							{
								//help other thread with cleanup
								cleanUp(deleteKey,s);
							}
						}
					}
				}
			}
			else
			{
				//in the cleanup phase
				//check if leaf is still present in the tree. If nobody has helped with the clean up old leaf will be still hanging. So remove it
				if(s.leaf == leaf)
				{
					//do cleanup
					if(cleanUp(deleteKey,s))
					{
						return;
					}
				}
				else
				{
					//someone helped with my cleanup. So I'm done
					return;
				}
			}
		}
	}

	public int setTag(int stamp)
	{
		switch(stamp) // set only tag
		{
		case 0:
			stamp = 1; // 00 to 01
			break;
		case 2:
			stamp = 3; // 10 to 11
			break;
		}
		return stamp;
	}

	public int copyFlag(int stamp)
	{
		switch(stamp) //copy only the flag
		{
		case 1:
			stamp = 0; // 01 to 00
			break;
		case 3:
			stamp = 2; // 11 to 10
			break;
		}
		return stamp;
	}
	
	public final boolean cleanUp(long key, SeekRecord s)
	{
		Node ancestor = s.ancestor;
		Node parent = s.parent;
		Node oldSuccessor;
		int oldStamp;
		Node sibling;
		int siblingStamp;

		if(key < parent.key) //xl case
		{
			if(parent.lChild.getStamp() > 1 ) // check if parent to leaf edge is already flagged. 10 or 11
			{
				//leaf node is flagged for deletion. tag the sibling edge to prevent any modification at this edge now
				sibling = parent.rChild.getReference();
				siblingStamp = parent.rChild.getStamp();
				siblingStamp = setTag(siblingStamp); // set only tag				
				parent.rChild.attemptStamp(sibling, siblingStamp);
				sibling = parent.rChild.getReference();
				siblingStamp = parent.rChild.getStamp();
			}
			else
			{				
				//leaf node is not flagged. So sibling node must have been flagged for deletion	
				sibling = parent.lChild.getReference();
				siblingStamp = parent.lChild.getStamp();
				siblingStamp = setTag(siblingStamp); // set only tag
				parent.lChild.attemptStamp(sibling, siblingStamp);
				sibling = parent.lChild.getReference();
				siblingStamp = parent.lChild.getStamp();
			}		
		}
		else //xr case
		{
			if(parent.rChild.getStamp() > 1 ) // check if parent to leaf edge is already flagged. 10 or 11
			{
				//leaf node is flagged for deletion. tag the sibling edge to prevent any modification at this edge now
				sibling = parent.lChild.getReference();
				siblingStamp = parent.lChild.getStamp();
				siblingStamp = setTag(siblingStamp); // set only tag				
				parent.lChild.attemptStamp(sibling, siblingStamp);
				sibling = parent.lChild.getReference();
				siblingStamp = parent.lChild.getStamp();
			}
			else
			{				
				//leaf node is not flagged. So sibling node must have been flagged for deletion	
				sibling = parent.rChild.getReference();
				siblingStamp = parent.rChild.getStamp();
				siblingStamp = setTag(siblingStamp); // set only tag
				parent.rChild.attemptStamp(sibling, siblingStamp);
				sibling = parent.rChild.getReference();
				siblingStamp = parent.rChild.getStamp();
			}		
		}

		if(key < ancestor.key)
		{
			siblingStamp = copyFlag(siblingStamp); //copy only the flag	
			oldSuccessor = ancestor.lChild.getReference();
			oldStamp = ancestor.lChild.getStamp();
			return(ancestor.lChild.compareAndSet(oldSuccessor, sibling, oldStamp, siblingStamp));	 
		}
		else
		{
			siblingStamp = copyFlag(siblingStamp); //copy only the flag	
			oldSuccessor = ancestor.rChild.getReference();
			oldStamp = ancestor.rChild.getStamp();
			return(ancestor.rChild.compareAndSet(oldSuccessor, sibling, oldStamp, siblingStamp));
		}
	}
			
	public final SeekRecord seek(long key)
	{
		AtomicStampedReference<Node> parentField;
		AtomicStampedReference<Node> currentField;
		Node current;

		//initialize the seek record
		SeekRecord s = new SeekRecord(grandParentHead, parentHead, parentHead, parentHead.lChild.getReference());

		parentField = s.ancestor.lChild;
		currentField = s.successor.lChild;

		while(currentField != null)
		{
			current = currentField.getReference();
			//move down the tree
			//check if the edge from the current parent node in the access path is tagged
			if(parentField.getStamp() == 0 || parentField.getStamp() == 2) // 00, 10 untagged
			{
				s.ancestor = s.parent;
				s.successor = s.leaf;
			}
			//advance parent and leaf pointers
			s.parent = s.leaf;
			s.leaf = current;
			parentField = currentField;
			if(key < current.key)
			{
				currentField = current.lChild;
			}
			else
			{
				currentField = current.rChild;
			}
		}
		return s;
	}

	public final void nodeCount(Node node)
	{
		if(node == null || node.key == 0)
		{
			return;
		}
		if(node.lChild == null)
		{
			nodeCount++;
		}

		if(node.lChild != null)
		{
			nodeCount(node.lChild.getReference());
			nodeCount(node.rChild.getReference());
		}
	}

	public final void createHeadNodes()
	{
		long key = Long.MAX_VALUE;
		long value = Long.MAX_VALUE;

		parentHead = new Node(key, value, new AtomicStampedReference<Node>(new Node(key,value), 0),new AtomicStampedReference<Node>(new Node(key,value), 0));
		grandParentHead = new Node(key, value, new AtomicStampedReference<Node>(parentHead, 0),new AtomicStampedReference<Node>(new Node(key,value), 0));
	}
	
}