import java.util.Random;

public class TestLockFreeBST implements Runnable
{
	int threadId;
	public static int NUM_OF_THREADS;
	public static int findPercent,insertPercent,deletePercent;
	public static int iterations,keyRange;
	static LockFreeBST obj;
	static long timeArray[];
	static double Mops;

	public TestLockFreeBST(int threadId)
	{
		this.threadId = threadId;
	}

	final void generateInput(int threadId)
	{
		int chooseOperation;
		long start,end;
		Random rd = new Random();
		rd.setSeed(threadId);
		if(threadId == 0)
		{
			for(int i=0;i<iterations/2;i++)
			{
				obj.insert(rd.nextInt(keyRange)+1, rd.nextInt(keyRange)+1);
			}
		}
		else
		{
			start = System.currentTimeMillis();
			for(int i=0;i<iterations;i++)
			{
				chooseOperation = rd.nextInt(100);

				if(chooseOperation < findPercent)
				{		
					obj.lookup(rd.nextInt(keyRange)+1);
				}
				else if (chooseOperation < insertPercent)
				{
					obj.insert(rd.nextInt(keyRange)+1, rd.nextInt(keyRange)+1);
				}
				else
				{
					obj.delete(rd.nextInt(keyRange)+1);
				}

			}
			end = System.currentTimeMillis();
			TestLockFreeBST.timeArray[threadId-1] = end-start;
		}
	}

	public void run()
	{
		generateInput(this.threadId);
	}

	public static void main(String[] args)
	{
		NUM_OF_THREADS 	= Integer.parseInt(args[0]);
		findPercent 	= Integer.parseInt(args[1].substring(0, 2));
		insertPercent 	= findPercent   + Integer.parseInt(args[1].substring(3, 5));
		deletePercent 	= insertPercent + Integer.parseInt(args[1].substring(6, 8));
		iterations 		= (int) Math.pow(2,Integer.parseInt(args[2]));
		keyRange 		= (int) Math.pow(2,Integer.parseInt(args[3]));
		
		TestLockFreeBST.timeArray = new long[NUM_OF_THREADS];
		try
		{
			obj = new LockFreeBST();
			obj.createHeadNodes();
			Thread[] arrayOfThreads = new Thread[NUM_OF_THREADS+1];

			arrayOfThreads[0] = new Thread(new TestLockFreeBST(0)); //just inserts - initial array
			arrayOfThreads[0].start();
			arrayOfThreads[0].join();


			for(int i=1;i<=NUM_OF_THREADS;i++)
			{
				arrayOfThreads[i] = new Thread(  new TestLockFreeBST(i));
				arrayOfThreads[i].start();
			}


			for(int i=1;i<=NUM_OF_THREADS;i++)
			{
				arrayOfThreads[i].join();
			}
			
			obj.nodeCount(LockFreeBST.grandParentHead);
			long totalTime=0;
			float avgTime=0.0f;
			for(int i=0;i<NUM_OF_THREADS;i++)
			{
				totalTime += TestLockFreeBST.timeArray[i];
			}
			
			avgTime = (totalTime * 1.0f) /(NUM_OF_THREADS * 1.0f);
			Mops = (long)iterations * NUM_OF_THREADS * NUM_OF_THREADS * 1.0d /(totalTime * 1000.0d);
			
			System.out.print("k" + args[3] + ";" + args[1] + ";" + args[0] + ";" + LockFreeBST.nodeCount);
			System.out.printf(";%.2f;%.2f;\n",avgTime,Mops);
		}
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}

	}
}
