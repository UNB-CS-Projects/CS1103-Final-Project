
/**
 * Write a description of class test here.
 *
 * @author (your name)
 * @version (a version number or a date)
 */
public class test
{
   public static void sortINT(int[] array)
   {
       int n = array.length;
       for (int i = 1; i<n;i++)
       {
           int key = array[i];
           int j = i-1;
           while (j >= 0 && array[j]>key)
           {
               array[j+1] = array[j];
               j = j-1;
               
           }
           array[j+1] = key;
       }
   }
}


