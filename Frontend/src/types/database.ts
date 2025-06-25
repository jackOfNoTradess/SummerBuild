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
      participates: {
        Row: {
          id: string;
          user_id: string;
          event_id: string;
          created_at: string;
          updated_at: string;
        };
        Insert: {
          id?: string;
          user_id: string;
          event_id: string;
          created_at?: string;
          updated_at?: string;
        };
        Update: {
          id?: string;
          user_id?: string;
          event_id?: string;
          updated_at?: string;
        };
      };
    };
  };
}

export type User = Database['public']['Tables']['users']['Row'];
export type Event = Database['public']['Tables']['events']['Row'];
export type Participation = Database['public']['Tables']['participates']['Row'];

// API Response types (with camelCase field names from backend)
export interface ApiEventResponse {
  id: string;
  title: string;
  hostUuid: string;
  capacity: number | null;
  startTime: string;
  endTime: string;
  description: string | null;
  tags: string[] | null;
  createdAt: string;
  updatedAt: string;
}

// Utility type that handles both DB and API formats
export type EventData = Event | ApiEventResponse;
