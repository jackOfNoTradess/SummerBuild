export interface Database {
  public: {
    Tables: {
      profiles: {
        Row: {
          id: string;
          email: string;
          full_name: string;
          avatar_url?: string;
          role: 'student' | 'organizer' | 'admin';
          created_at: string;
          updated_at: string;
        };
        Insert: {
          id: string;
          email: string;
          full_name: string;
          avatar_url?: string;
          role?: 'student' | 'organizer' | 'admin';
          created_at?: string;
          updated_at?: string;
        };
        Update: {
          id?: string;
          email?: string;
          full_name?: string;
          avatar_url?: string;
          role?: 'student' | 'organizer' | 'admin';
          updated_at?: string;
        };
      };
      events: {
        Row: {
          id: string;
          title: string;
          description: string;
          event_date: string;
          event_time: string;
          location: string;
          capacity: number;
          registered_count: number;
          image_url?: string;
          tags: string[];
          organizer_id: string;
          registration_deadline: string;
          additional_details?: string;
          created_at: string;
          updated_at: string;
        };
        Insert: {
          id?: string;
          title: string;
          description: string;
          event_date: string;
          event_time: string;
          location: string;
          capacity: number;
          registered_count?: number;
          image_url?: string;
          tags: string[];
          organizer_id: string;
          registration_deadline: string;
          additional_details?: string;
          created_at?: string;
          updated_at?: string;
        };
        Update: {
          id?: string;
          title?: string;
          description?: string;
          event_date?: string;
          event_time?: string;
          location?: string;
          capacity?: number;
          registered_count?: number;
          image_url?: string;
          tags?: string[];
          organizer_id?: string;
          registration_deadline?: string;
          additional_details?: string;
          updated_at?: string;
        };
      };
      bookings: {
        Row: {
          id: string;
          event_id: string;
          user_id: string;
          booking_reference: string;
          tickets_quantity: number;
          status: 'confirmed' | 'cancelled';
          created_at: string;
        };
        Insert: {
          id?: string;
          event_id: string;
          user_id: string;
          booking_reference: string;
          tickets_quantity: number;
          status?: 'confirmed' | 'cancelled';
          created_at?: string;
        };
        Update: {
          id?: string;
          event_id?: string;
          user_id?: string;
          booking_reference?: string;
          tickets_quantity?: number;
          status?: 'confirmed' | 'cancelled';
        };
      };
    };
  };
}

export type Profile = Database['public']['Tables']['profiles']['Row'];
export type Event = Database['public']['Tables']['events']['Row'];
export type Booking = Database['public']['Tables']['bookings']['Row'];