import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class PPMReadWrite {

        static int[] dimension=new int[2];  //Images dimension
        static File[] imagesFiles=new File[2];      //starting images
        static int morphCount;

        private static RGBImage read(String filename) throws IOException{
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(filename)));
            // deal with header
            String p3 = reader.readLine();      //header/comment not needed
            reader.readLine();
            String dimensions = reader.readLine();
            Pattern dimensionPattern = Pattern.compile("(\\d+) (\\d+)");
            Matcher dimensionMatcher = dimensionPattern.matcher(dimensions);
            String range = reader.readLine();
            if(dimensionMatcher.matches()){
                int numcolumns = Integer.parseInt(dimensionMatcher.group(1));
                int numrows = Integer.parseInt(dimensionMatcher.group(2));
                short[][] r = new short[numrows][numcolumns];
                short[][] g = new short[numrows][numcolumns];
                short[][] b = new short[numrows][numcolumns];
                String line;
                int loc = 0; // will range to rowdim*columndim;
                int row;
                int column;
                while((line = reader.readLine())!=null){
                    String[] numbers = line.split("\\s+");
                    for(int i=0;i<numbers.length;i++){
                        int rawloc = loc / 3;
                        row = rawloc / numcolumns;
                        column = rawloc % numcolumns;
                        int color = loc % 3;
                        if(color == 0) {
                            r[row][column] = Short.parseShort(numbers[i]);
                        }else if( color ==1){
                            g[row][column] =  Short.parseShort(numbers[i]);
                        } else if (color ==2){
                            b[row][column] =  Short.parseShort(numbers[i]);
                        }
                        loc += 1;
                    }
                }
                return new RGBImage(r, g, b);
            } else {
                throw new IOException("could not read this; maybe it's not an ascii rgb ppm?"+dimensions);
            }
        }


        public static void  write(RGBImage image, String filename) throws IOException{
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename)));
            // write header
            int rowdimension = image.getNumrows();
            int columndimension = image.getNumcolumns();
            writer.write("P3");
            writer.newLine();
            writer.write("# Created by GIMP version 2.10.6 PNM plug-in");
            writer.newLine();
            writer.write(image.getNumcolumns()+" "+image.getNumrows());
            writer.newLine();
            writer.write("255");
            writer.newLine();
            for(int row=0;row<rowdimension;row++){
                for(int column=0;column<columndimension;column++){
                    writer.write(image.getRed()[row][column]+" ");
                    writer.write(image.getGreen()[row][column]+" ");
                    writer.write(image.getBlue()[row][column]+"");
                    if(column < columndimension - 1)writer.write(" ");
                }
                writer.newLine();
            }
            writer.flush();
            writer.close();
        }

        //Find fitting dimension for both images
        private static void dimension(RGBImage image1, RGBImage image2)
        {
            int heigh=0, width=0;
            if(image1.getNumcolumns()>image2.getNumcolumns())
            {
                heigh=image2.getNumcolumns();
                image1.setNumcolumns(heigh);
            }else
            {
                heigh=image1.getNumcolumns();
                image2.setNumcolumns(heigh);
            }

            if(image1.getNumrows()>image2.getNumrows())
            {
                width=image2.getNumrows();
                image1.setNumrows(width);
            }else
            {
                width=image1.getNumrows();
                image2.setNumrows(width);
            }
            dimension[0]=width;dimension[1]=heigh;
        }

        //Find the average of each color pixel to found new images
        private static RGBImage[] morph(RGBImage leftImage, RGBImage rightImage, RGBImage[] imageArray, int start, int end)
        {
            RGBImage newImage;      //temporarily image to hold

            //Each RGB color in the image
            short[][] r=new short[dimension[0]][dimension[1]];
            short[][] g=new short[dimension[0]][dimension[1]];
            short[][] b=new short[dimension[0]][dimension[1]];

            //Calculate the average of image1 and image2 and store the RGB
            for(int row=0;row<dimension[0];row++)
            {
                for(int col=0; col<dimension[1];col++)
                {
                    r[row][col]=(short)((leftImage.getRed()[row][col]+rightImage.getRed()[row][col])/2);
                    g[row][col]=(short)((leftImage.getGreen()[row][col]+rightImage.getGreen()[row][col])/2);
                    b[row][col]=(short)((leftImage.getBlue()[row][col]+rightImage.getBlue()[row][col])/2);
                }
            }
            //Assign the RGB to newImage
            newImage=new RGBImage(r,g,b);

            int mid=(((start+1)+(end))/2);  //Divide RGBArray in half

            //stop recursion if the mid pointer is the same as the end pointer
            if(mid==start || mid==end)
            {
                return imageArray;
            }else{
            imageArray[mid]=newImage;       //Add newly found image at the middle of the imageArray
            morph(imageArray[start],imageArray[mid],imageArray, start,mid);         //Divide left half of the Array
            morph(imageArray[mid],imageArray[end],imageArray,mid,end);}             //Divide right half of Array
            return imageArray;
        }


        public PPMReadWrite(File image1, File image2, int morphCount)
        {

            imagesFiles[0]=image1; imagesFiles[1]=image2;
            this.morphCount=morphCount;
        }

        //Return array of all possible RGBImages
        public static RGBImage[] getMorphImages() throws IOException
        {

            RGBImage[] images=new RGBImage[morphCount];             //Total Morphed image location
            RGBImage image1=read(imagesFiles[0].getPath());         //Convert .ppm file to RGBImage
            RGBImage image2=read(imagesFiles[1].getPath());         //Convert .ppm file to RGBImage
            dimension(image1,image2);           //return fitting dimension for both images if images are unequal size
            images[0]=image1;           //Insert Image1 at the beginning of RGBImage Array
            images[images.length-1]=image2;             //Insert Image2 at the end of RGBImage Array
            return morph(image1,image2,images, 0,images.length-1);      //Start recursion and return RGBImage array
        }


        //Return BufferImages for displaying on JFrame if needed
        public static BufferedImage[] getBufferImage()throws IOException
        {
            RGBImage[] images=getMorphImages();
            BufferedImage[] imagesArray=new BufferedImage[images.length];

            for(int imagesCount=0;imagesCount<images.length;imagesCount++)
            {
                imagesArray[imagesCount]=new BufferedImage(dimension[0], dimension[1], BufferedImage.TYPE_3BYTE_BGR);       //Array of all possible Morph images
                //copy pixel from RGBImage Array to bufferImages for each BufferImage
                for(int eachImageRow=0;eachImageRow<dimension[0];eachImageRow++)
                {
                    for(int eachImageCol=0;eachImageCol<dimension[1];eachImageCol++)
                    {
                        imagesArray[imagesCount].setRGB(eachImageRow,eachImageCol,new Color(
                                images[imagesCount].getRed()[eachImageRow][eachImageCol],images[imagesCount].getGreen()[eachImageRow][eachImageCol],
                                images[imagesCount].getBlue()[eachImageRow][eachImageCol],255).getRGB());
                    }
                }
            }
            return imagesArray;
        }

    }
