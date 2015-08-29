import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Log {
	private static Log instance = null;
    
    private BufferedWriter bw;
    
	protected Log()
	{
        try
        {
            File file = new File("tetris.log");
            
            if (!file.exists())
            {
                file.createNewFile();
            }
            
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            bw = new BufferedWriter(fw);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
	}
    
	public static Log getInstance()
	{
		if (instance == null)
		{
			instance = new Log();
		}
		return instance;
	}
    
    public void Write(String msg)
    {
        try
        {
            bw.write(msg);
            bw.newLine();
            bw.flush();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

}

