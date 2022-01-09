package com.example.facediscern;

import java.util.List;

public class Bean {
    public String face_num;
    public List<Face> faces;
    public String image_id;
    public String request_id;
    public String time_used;

    @Override
    public String toString() {
        return "Bean{" +
                "face_num='" + face_num + '\'' +
                ", faces=" + faces +
                ", image_id='" + image_id + '\'' +
                ", request_id='" + request_id + '\'' +
                ", time_used='" + time_used + '\'' +
                '}';
    }

    public  class Face {
        public String face_token;
        public Rectangle face_rectangle;
        public Attributes attributes;

        @Override
        public String toString() {
            return "Face{" +
                    "face_token='" + face_token + '\'' +
                    ", face_rectangle=" + face_rectangle +
                    ", attributes=" + attributes +
                    '}';
        }

        public  class Rectangle {
            public String top;
            public String left;
            public String width;
            public String height;
        }

        public  class Attributes {
            public Gender gender;
            public Age age;
            public Beauty beauty;
            public SkinStatus skinstatus;

            @Override
            public String toString() {
                return "Attributes{" +
                        "gender=" + gender +
                        ", age=" + age +
                        ", beauty=" + beauty +
                        ", skinstatus=" + skinstatus +
                        '}';
            }

            public  class Gender {
                public String value;
            }

            public  class Age {
                public String value;
            }

            public  class Beauty {
                public String male_score;
                public String female_score;
            }

            public  class SkinStatus {
                public String health;
                public String stain;
                public String dark_circle;
                public String acne;
            }
        }
    }
}
