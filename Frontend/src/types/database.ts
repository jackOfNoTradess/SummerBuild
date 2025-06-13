export interface Database {
  public: {
    Tables: {
      users: {
        Row: {
          id: string;
          role: 'USER' | 'ADMIN' | 'ORGANIZER';
          gender: 'MALE' | 'FEMALE' | 'OTHERS';
          created_at: string;
          updated_at: string;
        };
        Insert: {
          id?: string;
          role: 'USER' | 'ADMIN' | 'ORGANIZER';
          gender: 'MALE' | 'FEMALE' | 'OTHERS';
          created_at?: string;
          updated_at?: string;
        };
        Update: {
          id?: string;
          role?: 'USER' | 'ADMIN' | 'ORGANIZER';
          gender?: 'MALE' | 'FEMALE' | 'OTHERS';
          updated_at?: string;
        };
      };
      events: {
        Row: {
          id: string;
          title: string;
          host_id: string;
          capacity: number | null;
          start_time: string;
          end_time: string;
          description: string | null;
          tag: string[] | null;
          pic_path: string | null;
          created_at: string;
          updated_at: string;
        };
        Insert: {
          id?: string;
          title: string;
          host_id: string;
          capacity?: number | null;
          start_time: string;
          end_time: string;
          description?: string | null;
          tag?: string[] | null;
          pic_path?: string | null;
          created_at?: string;
          updated_at?: string;
        };
        Update: {
          id?: string;
          title?: string;
          host_id?: string;
          capacity?: number | null;
          start_time?: string;
          end_time?: string;
          description?: string | null;
          tag?: string[] | null;
          pic_path?: string | null;
          updated_at?: string;
        };
      };
    };
  };
}

export type User = Database['public']['Tables']['users']['Row'];
export type Event = Database['public']['Tables']['events']['Row'];

// Legacy type aliases for backward compatibility
export type Profile = User;
